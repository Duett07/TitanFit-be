package com.app.be.service.impl;

import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.OrderItemRequest;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.OrderStatus;
import com.app.be.dto.order.UpdateOrderStatusRequest;
import com.app.be.entity.Order;
import com.app.be.entity.OrderItem;
import com.app.be.entity.ProductVariant;
import com.app.be.exception.BadRequestException;
import com.app.be.exception.ResourceNotFoundException;
import com.app.be.exception.UnauthorizedException;
import com.app.be.repository.OrderItemRepository;
import com.app.be.repository.OrderRepository;
import com.app.be.repository.ProductVariantRepository;
import com.app.be.repository.UserRepository;
import com.app.be.security.user.CustomUserDetails;
import com.app.be.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ORDER_CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int ORDER_CODE_LENGTH = 8;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, CustomUserDetails userDetails) {
        // Bước 1: Sinh mã đơn hàng duy nhất: TF- + 6 chữ số ngẫu nhiên
        String orderCode = generateUniqueOrderCode();

        // Bước 2 & 3: Lặp qua từng item, kiểm tra tồn kho, trừ kho tức thì và tính tổng tiền từ DB
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemRequest.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể sản phẩm với ID: " + itemRequest.getVariantId()));

            // Kiểm tra: Nếu variant.getStockQuantity() < item.getQuantity() -> ném BadRequestException
            if (variant.getStockQuantity() < itemRequest.getQuantity()) {
                throw new BadRequestException("Sản phẩm " + variant.getVariantName() + " không đủ hàng tồn kho!");
            }

            // Trừ kho tức thì
            variant.setStockQuantity(variant.getStockQuantity() - itemRequest.getQuantity());
            productVariantRepository.save(variant);

            // Bước 3: Lấy giá trực tiếp từ database (variant.getPrice()) để tính tổng tiền
            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            // Chuẩn bị snapshot cho OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setProductVariant(variant);
            String productName = (variant.getProduct() != null && variant.getProduct().getName() != null)
                    ? variant.getProduct().getName()
                    : variant.getVariantName();
            orderItem.setProductName(productName);
            orderItem.setVariantName(variant.getVariantName());
            orderItem.setPrice(unitPrice);
            orderItem.setQuantity(itemRequest.getQuantity());

            orderItems.add(orderItem);
        }

        // Bước 4: Lưu Order với trạng thái ban đầu order_status = 'PENDING', payment_status = 'UNPAID'
        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setCustomerName(request.getCustomerName().trim());
        order.setPhone(request.getPhone().trim());
        order.setShippingAddress(request.getShippingAddress().trim());
        order.setNote(request.getNote() != null ? request.getNote().trim() : null);
        order.setPaymentMethod(request.getPaymentMethod().name());
        order.setOrderStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        order.setTotalAmount(totalAmount);

        // Gắn thông tin người dùng nếu có tài khoản
        if (userDetails != null && userDetails.getId() != null) {
            userRepository.findById(userDetails.getId()).ifPresent(order::setUser);
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails principalUser) {
                userRepository.findById(principalUser.getId()).ifPresent(order::setUser);
            }
        }

        Order savedOrder = orderRepository.save(order);

        // Bước 5: Lưu danh sách OrderItem lưu lại snapshot tên và giá tại thời điểm mua
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);
        savedOrder.setOrderItems(savedItems);

        log.info("Order created successfully with code: {} | Total: {} | Items count: {}",
                savedOrder.getOrderCode(), savedOrder.getTotalAmount(), savedItems.size());

        return OrderResponse.fromEntity(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        return createOrder(request, null);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode, CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new UnauthorizedException("Người dùng chưa được xác thực");
        }

        if (orderCode == null || orderCode.trim().isEmpty()) {
            throw new BadRequestException("Mã đơn hàng không được để trống");
        }

        Order order = orderRepository.findWithDetailsByOrderCode(orderCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode.trim()));

        boolean isAdmin = userDetails.getAuthorities() != null && userDetails.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equalsIgnoreCase(auth.getAuthority()));

        boolean isOwner = order.getUser() != null && userDetails.getId().equals(order.getUser().getId());

        if (!isAdmin && !isOwner) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode.trim());
        }

        return OrderResponse.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode) {
        if (orderCode == null || orderCode.trim().isEmpty()) {
            throw new BadRequestException("Mã đơn hàng không được để trống");
        }

        Order order = orderRepository.findWithDetailsByOrderCode(orderCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode.trim()));

        return OrderResponse.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new UnauthorizedException("Người dùng chưa được xác thực");
        }

        List<Order> orders = orderRepository.findByUser_IdOrderByCreatedAtDesc(userDetails.getId());
        return orders.stream()
                .map(OrderResponse::fromEntity)
                .toList();
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
            throw new BadRequestException("Trạng thái mới không được để trống");
        }

        if (newStatus == OrderStatus.PENDING) {
            throw new BadRequestException("Không thể chuyển đơn hàng về trạng thái PENDING");
        }

        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (order.getOrderStatus() != null && order.getOrderStatus().equalsIgnoreCase(newStatus.name())) {
            throw new BadRequestException("Đơn hàng đã ở trạng thái " + newStatus.name() + " rồi");
        }

        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BadRequestException("Đơn hàng đã bị HỦY trước đó, không thể thay đổi trạng thái");
        }

        if ("COMPLETED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BadRequestException("Đơn hàng đã HOÀN THÀNH, không thể thay đổi trạng thái");
        }

        // Đặc biệt: Nếu newStatus == 'CANCELLED' -> Tự động duyệt qua các OrderItem và cộng hoàn trả lại stock_quantity cho từng ProductVariant!
        if (newStatus == OrderStatus.CANCELLED) {
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    ProductVariant variant = item.getProductVariant();
                    if (variant != null) {
                        int currentStock = variant.getStockQuantity();
                        int restoredStock = currentStock + item.getQuantity();
                        variant.setStockQuantity(restoredStock);
                        productVariantRepository.save(variant);
                        log.info("Hoàn trả tồn kho cho SKU '{}': {} + {} -> {}",
                                variant.getSku(), currentStock, item.getQuantity(), restoredStock);
                    }
                }
            }
        }

        order.setOrderStatus(newStatus.name());
        Order savedOrder = orderRepository.save(order);

        log.info("Cập nhật đơn hàng {} sang trạng thái {}", savedOrder.getOrderCode(), newStatus.name());
        return OrderResponse.fromEntity(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request, OrderStatus paramStatus) {
        OrderStatus targetStatus = null;
        if (request != null && request.getNewStatus() != null) {
            targetStatus = request.getNewStatus();
        } else if (paramStatus != null) {
            targetStatus = paramStatus;
        }

        if (targetStatus == null) {
            throw new BadRequestException("Trạng thái mới (newStatus) không được để trống (CONFIRMED, SHIPPING, COMPLETED, CANCELLED)");
        }

        return updateOrderStatus(orderId, targetStatus);
    }

    /**
     * Sinh mã đơn hàng duy nhất có độ hỗn loạn cao bằng SecureRandom
     * Định dạng: TF- + 8 ký tự alphanumeric Base32 (ví dụ: TF-7K9M2PX4)
     * Không gian mẫu: 32^8 ~ 1.1 x 10^12 khả năng, loại bỏ nguy cơ dò quét hàng loạt.
     */
    private String generateUniqueOrderCode() {
        String orderCode;
        do {
            StringBuilder sb = new StringBuilder("TF-");
            for (int i = 0; i < ORDER_CODE_LENGTH; i++) {
                int index = SECURE_RANDOM.nextInt(ORDER_CODE_CHARS.length());
                sb.append(ORDER_CODE_CHARS.charAt(index));
            }
            orderCode = sb.toString();
        } while (orderRepository.existsByOrderCode(orderCode));
        return orderCode;
    }
}
