package com.app.be.service.impl;

import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.OrderItemRequest;
import com.app.be.dto.order.OrderResponse;
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
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

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
    public OrderResponse getOrderByCode(String orderCode) {
        Order order = orderRepository.findWithDetailsByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode));

        return OrderResponse.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new UnauthorizedException("Người dùng chưa được xác thực");
        }

        List<Order> orders = orderRepository.findByUser_Id(userDetails.getId());
        return orders.stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    /**
     * Sinh mã đơn hàng duy nhất có định dạng TF- + 6 chữ số ngẫu nhiên (ví dụ: TF-883921)
     */
    private String generateUniqueOrderCode() {
        String orderCode;
        do {
            int randomNum = ThreadLocalRandom.current().nextInt(100000, 1000000);
            orderCode = "TF-" + randomNum;
        } while (orderRepository.existsByOrderCode(orderCode));
        return orderCode;
    }
}
