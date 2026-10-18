package com.app.be.service.impl;

import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.TrackOrderRequest;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        // Bước 1: Sinh mã đơn hàng duy nhất: TF- + 8 ký tự Base32
        String orderCode = generateUniqueOrderCode();

        // Bước 2 & 3: Gom nhóm số lượng theo variantId, sắp xếp ID để tránh deadlock khi tranh chấp lock,
        // kiểm tra tồn kho bằng Pessimistic Lock (SELECT FOR UPDATE) và trừ kho an toàn
        Map<UUID, Integer> aggregatedQuantities = new LinkedHashMap<>();
        for (OrderItemRequest itemRequest : request.getItems()) {
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

            // Khóa dòng biến thể (SELECT ... FOR UPDATE) chống race condition
            ProductVariant variant = productVariantRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể sản phẩm với ID: " + variantId));

            // Kiểm tra tồn kho dưới khóa bi quan
            if (variant.getStockQuantity() < quantity) {
                throw new BadRequestException("Sản phẩm '" + variant.getVariantName() + "' không đủ hàng tồn kho (yêu cầu: " + quantity + ", còn lại: " + variant.getStockQuantity() + ")!");
            }

            // Trừ kho an toàn dưới khóa bi quan
            variant.setStockQuantity(variant.getStockQuantity() - quantity);
            productVariantRepository.save(variant);

            // Bước 3: Lấy giá trực tiếp từ database (variant.getPrice()) để tính tổng tiền
            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
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
            orderItem.setQuantity(quantity);

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
    public OrderResponse trackOrder(TrackOrderRequest request) {
        if (request == null || request.getOrderCode() == null || request.getOrderCode().trim().isEmpty()) {
            throw new BadRequestException("Mã đơn hàng không được để trống");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new BadRequestException("Số điện thoại không được để trống");
        }

        Order order = orderRepository.findWithDetailsByOrderCode(request.getOrderCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc số điện thoại không trùng khớp"));

        String inputPhone = normalizePhone(request.getPhone());
        String orderPhone = normalizePhone(order.getPhone());

        if (!inputPhone.equals(orderPhone)) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc số điện thoại không trùng khớp");
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
                // Gom nhóm số lượng hoàn kho theo variantId
                Map<UUID, Integer> restoreQuantities = new LinkedHashMap<>();
                for (OrderItem item : order.getOrderItems()) {
                    if (item.getProductVariant() != null && item.getProductVariant().getId() != null) {
                        restoreQuantities.merge(item.getProductVariant().getId(), item.getQuantity(), Integer::sum);
                    }
                }

                // Sắp xếp thứ tự ID để khóa theo thứ tự nhất quán, tránh deadlock
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
                        productVariantRepository.save(variant);
                        log.info("Hoàn trả tồn kho cho SKU '{}' (ID: {}): {} + {} -> {}",
                                variant.getSku(), variantId, currentStock, qtyToRestore, restoredStock);
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
