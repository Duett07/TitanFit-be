package com.app.be.service.impl;

import com.app.be.constant.OrderMessages;
import com.app.be.dto.common.PageResponse;
import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.OrderItemRequest;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.TrackOrderRequest;
import com.app.be.entity.Order;
import com.app.be.entity.OrderItem;
import com.app.be.entity.ProductVariant;
import com.app.be.entity.User;
import com.app.be.enums.OrderStatus;
import com.app.be.enums.PaymentMethod;
import com.app.be.enums.PaymentStatus;
import com.app.be.exception.BadRequestException;
import com.app.be.exception.ResourceNotFoundException;
import com.app.be.exception.UnauthorizedException;
import com.app.be.repository.OrderRepository;
import com.app.be.repository.ProductVariantRepository;
import com.app.be.repository.UserRepository;
import com.app.be.repository.specification.OrderSpecification;
import com.app.be.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ORDER_CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int ORDER_CODE_LENGTH = 8;
    private static final int MAX_GENERATE_ORDER_CODE_ATTEMPTS = 10;

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductVariantRepository productVariantRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, UUID userId) {
        User user = resolveCustomer(userId);
        ReservedOrderItems reserved = reserveStockAndBuildItems(request.getItems());
        String orderCode = generateUniqueOrderCode();
        Order order = buildOrder(request, user, orderCode, reserved.totalAmount(), reserved.items());

        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully with code: {} | Total: {} | Items count: {}",
                savedOrder.getOrderCode(), savedOrder.getTotalAmount(), savedOrder.getOrderItems().size());

        return OrderResponse.fromEntity(savedOrder);
    }

    private User resolveCustomer(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException(OrderMessages.LOGIN_REQUIRED);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(OrderMessages.USER_NOT_FOUND_OR_LOCKED));
    }

    private record ReservedOrderItems(List<OrderItem> items, BigDecimal totalAmount) {}

    private ReservedOrderItems reserveStockAndBuildItems(List<OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new BadRequestException("Danh sách sản phẩm đặt hàng không được để trống");
        }

        Map<UUID, Integer> aggregatedQuantities = new LinkedHashMap<>();
        for (OrderItemRequest itemRequest : itemRequests) {
            if (itemRequest.getVariantId() != null && itemRequest.getQuantity() != null) {
                aggregatedQuantities.merge(itemRequest.getVariantId(), itemRequest.getQuantity(), Integer::sum);
            }
        }

        List<UUID> sortedVariantIds = new ArrayList<>(aggregatedQuantities.keySet());
        Collections.sort(sortedVariantIds);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (UUID variantId : sortedVariantIds) {
            int quantity = aggregatedQuantities.get(variantId);

            ProductVariant variant = productVariantRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException(OrderMessages.VARIANT_NOT_FOUND_PREFIX + variantId));

            if (variant.getProduct() == null || !variant.getProduct().isActive()) {
                String productName = (variant.getProduct() != null && variant.getProduct().getName() != null)
                        ? variant.getProduct().getName()
                        : variant.getVariantName();
                throw new BadRequestException(String.format(OrderMessages.PRODUCT_UNAVAILABLE, productName));
            }

            if (variant.getStockQuantity() < quantity) {
                throw new BadRequestException(String.format(
                        OrderMessages.INSUFFICIENT_STOCK,
                        variant.getVariantName(),
                        quantity,
                        variant.getStockQuantity()
                ));
            }

            variant.setStockQuantity(variant.getStockQuantity() - quantity);

            BigDecimal unitPrice = variant.getPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                String productName = (variant.getProduct() != null && variant.getProduct().getName() != null)
                        ? variant.getProduct().getName()
                        : variant.getVariantName();
                throw new BadRequestException(String.format(OrderMessages.INVALID_PRICE, productName));
            }
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductVariant(variant);
            String productName = (variant.getProduct() != null && variant.getProduct().getName() != null)
                    ? variant.getProduct().getName()
                    : variant.getVariantName();
            orderItem.setProductName(productName);
            orderItem.setVariantName(variant.getVariantName());
            orderItem.setPrice(unitPrice);
            orderItem.setQuantity(quantity);

            orderItems.add(orderItem);
        }

        return new ReservedOrderItems(orderItems, totalAmount);
    }

    private Order buildOrder(
            CreateOrderRequest request,
            User user,
            String orderCode,
            BigDecimal totalAmount,
            List<OrderItem> orderItems
    ) {
        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setUser(user);
        order.setCustomerName(request.getCustomerName().trim());
        order.setPhone(request.getPhone().trim());
        order.setShippingAddress(request.getShippingAddress().trim());
        order.setNote(request.getNote() != null ? request.getNote().trim() : null);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setTotalAmount(totalAmount);

        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.setOrderItems(orderItems);
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode, UUID userId, boolean isAdmin) {
        if (userId == null) {
            throw new UnauthorizedException(OrderMessages.USER_UNAUTHENTICATED);
        }

        if (orderCode == null || orderCode.trim().isEmpty()) {
            throw new BadRequestException(OrderMessages.ORDER_CODE_EMPTY);
        }

        Order order = orderRepository.findWithDetailsByOrderCode(orderCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException(OrderMessages.ORDER_NOT_FOUND_PREFIX + orderCode.trim()));

        boolean isOwner = order.getUser() != null && userId.equals(order.getUser().getId());

        if (!isAdmin && !isOwner) {
            throw new ResourceNotFoundException(OrderMessages.ORDER_NOT_FOUND_PREFIX + orderCode.trim());
        }

        return OrderResponse.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse trackOrder(TrackOrderRequest request) {
        if (request == null || request.getOrderCode() == null || request.getOrderCode().trim().isEmpty()) {
            throw new BadRequestException(OrderMessages.ORDER_CODE_EMPTY);
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new BadRequestException(OrderMessages.PHONE_EMPTY);
        }

        Order order = orderRepository.findWithDetailsByOrderCode(request.getOrderCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException(OrderMessages.ORDER_OR_PHONE_MISMATCH));

        String inputPhone = normalizePhone(request.getPhone());
        String orderPhone = normalizePhone(order.getPhone());

        if (!inputPhone.equals(orderPhone)) {
            throw new ResourceNotFoundException(OrderMessages.ORDER_OR_PHONE_MISMATCH);
        }

        return OrderResponse.fromEntity(order);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("84") && cleaned.length() == 11) {
            cleaned = "0" + cleaned.substring(2);
        }
        return cleaned;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(
            UUID userId,
            int page,
            int size,
            OrderStatus status
    ) {
        if (userId == null) {
            throw new UnauthorizedException(OrderMessages.USER_UNAUTHENTICATED);
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Order> spec = OrderSpecification.filterOrders(
                userId, status, null, null, null, null
        );

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        List<OrderResponse> content = orderPage.getContent().stream()
                .map(OrderResponse::fromEntity)
                .toList();

        return PageResponse.from(orderPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException(OrderMessages.USER_UNAUTHENTICATED);
        }

        List<Order> orders = orderRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(
            int page,
            int size,
            OrderStatus status,
            PaymentStatus paymentStatus,
            LocalDate startDate,
            LocalDate endDate,
            String search
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Order> spec = OrderSpecification.filterOrders(
                null, status, paymentStatus, startDate, endDate, search
        );

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        List<OrderResponse> content = orderPage.getContent().stream()
                .map(OrderResponse::fromEntity)
                .toList();

        return PageResponse.from(orderPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        return orders.stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        if (newStatus == null) {
            throw new BadRequestException(OrderMessages.STATUS_REQUIRED);
        }

        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(OrderMessages.ORDER_NOT_FOUND_BY_ID_PREFIX + orderId));

        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus == null) {
            throw new BadRequestException(OrderMessages.CURRENT_STATUS_INVALID);
        }

        if (currentStatus == newStatus) {
            throw new BadRequestException(String.format(OrderMessages.ALREADY_IN_STATUS, newStatus.name()));
        }

        if (!currentStatus.canTransitionTo(newStatus)) {
            if (currentStatus == OrderStatus.CANCELLED) {
                throw new BadRequestException(OrderMessages.CANCELLED_CANNOT_CHANGE);
            }
            if (currentStatus == OrderStatus.COMPLETED) {
                throw new BadRequestException(OrderMessages.COMPLETED_CANNOT_CHANGE);
            }
            throw new BadRequestException(String.format(
                    OrderMessages.INVALID_STATUS_TRANSITION,
                    currentStatus.name(),
                    newStatus.name(),
                    currentStatus.nextAllowedStatuses()
            ));
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restoreStockForCancelledOrder(order);

            // Nếu đơn hàng trước đó đã thanh toán (PAID), tự động chuyển sang REFUNDED để theo dõi đối soát
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                log.info("Đơn hàng {} bị hủy sau khi đã thanh toán, chuyển paymentStatus sang REFUNDED", order.getOrderCode());
            }
        }

        // Nếu chuyển sang COMPLETED: Với đơn COD và paymentStatus đang là UNPAID -> Tự động chuyển sang PAID
        if (newStatus == OrderStatus.COMPLETED) {
            if (order.getPaymentMethod() == PaymentMethod.COD && order.getPaymentStatus() == PaymentStatus.UNPAID) {
                order.setPaymentStatus(PaymentStatus.PAID);
                log.info("Đơn hàng COD {} đã hoàn thành, tự động cập nhật paymentStatus sang PAID", order.getOrderCode());
            }
        }

        order.setOrderStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        log.info("Cập nhật đơn hàng {} sang trạng thái {}", savedOrder.getOrderCode(), newStatus.name());
        return OrderResponse.fromEntity(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updatePaymentStatus(UUID orderId, PaymentStatus newStatus) {
        if (newStatus == null) {
            throw new BadRequestException(OrderMessages.PAYMENT_STATUS_REQUIRED);
        }

        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(OrderMessages.ORDER_NOT_FOUND_BY_ID_PREFIX + orderId));

        if (order.getPaymentStatus() == newStatus) {
            throw new BadRequestException(String.format(OrderMessages.ALREADY_IN_PAYMENT_STATUS, newStatus.name()));
        }

        order.setPaymentStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        log.info("Cập nhật trạng thái thanh toán đơn hàng {} sang {}", savedOrder.getOrderCode(), newStatus.name());
        return OrderResponse.fromEntity(savedOrder);
    }

    private void restoreStockForCancelledOrder(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        Map<UUID, Integer> restoreQuantities = new LinkedHashMap<>();
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProductVariant() != null && item.getProductVariant().getId() != null) {
                restoreQuantities.merge(item.getProductVariant().getId(), item.getQuantity(), Integer::sum);
            }
        }

        List<UUID> sortedVariantIds = new ArrayList<>(restoreQuantities.keySet());
        Collections.sort(sortedVariantIds);

        for (UUID variantId : sortedVariantIds) {
            int qtyToRestore = restoreQuantities.get(variantId);
            ProductVariant variant = productVariantRepository.findByIdForUpdate(variantId)
                    .orElse(null);
            if (variant != null) {
                int currentStock = variant.getStockQuantity();
                int restoredStock = currentStock + qtyToRestore;
                variant.setStockQuantity(restoredStock);
                log.info("Hoàn trả tồn kho cho SKU '{}' (ID: {}): {} + {} -> {}",
                        variant.getSku(), variantId, currentStock, qtyToRestore, restoredStock);
            }
        }
    }

    /**
     * Sinh mã đơn hàng duy nhất có độ hỗn loạn cao bằng SecureRandom
     * Định dạng: TF- + 8 ký tự alphanumeric Base32 (ví dụ: TF-7K9M2PX4)
     * Giới hạn tối đa MAX_GENERATE_ORDER_CODE_ATTEMPTS lần thử để tránh vòng lặp vô hạn.
     */
    private String generateUniqueOrderCode() {
        for (int attempt = 0; attempt < MAX_GENERATE_ORDER_CODE_ATTEMPTS; attempt++) {
            StringBuilder sb = new StringBuilder("TF-");
            for (int i = 0; i < ORDER_CODE_LENGTH; i++) {
                int index = SECURE_RANDOM.nextInt(ORDER_CODE_CHARS.length());
                sb.append(ORDER_CODE_CHARS.charAt(index));
            }
            String orderCode = sb.toString();
            if (!orderRepository.existsByOrderCode(orderCode)) {
                return orderCode;
            }
        }
        throw new IllegalStateException("Không thể sinh mã đơn hàng duy nhất sau " + MAX_GENERATE_ORDER_CODE_ATTEMPTS + " lần thử");
    }
}
