package com.app.be.service;

import com.app.be.dto.common.PageResponse;
import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.TrackOrderRequest;
import com.app.be.dto.order.UpdateOrderStatusRequest;
import com.app.be.dto.order.UpdatePaymentStatusRequest;
import com.app.be.enums.OrderStatus;
import com.app.be.enums.PaymentStatus;
import com.app.be.security.user.CustomUserDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, CustomUserDetails userDetails);

    OrderResponse getOrderByCode(String orderCode, CustomUserDetails userDetails);

    OrderResponse trackOrder(TrackOrderRequest request);

    PageResponse<OrderResponse> getMyOrders(
            CustomUserDetails userDetails,
            int page,
            int size,
            OrderStatus status
    );

    List<OrderResponse> getMyOrders(CustomUserDetails userDetails);

    OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus);

    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request, OrderStatus paramStatus);

    OrderResponse updatePaymentStatus(UUID orderId, PaymentStatus newStatus);

    OrderResponse updatePaymentStatus(UUID orderId, UpdatePaymentStatusRequest request, PaymentStatus paramStatus);

    PageResponse<OrderResponse> getAllOrders(
            int page,
            int size,
            OrderStatus status,
            PaymentStatus paymentStatus,
            LocalDate startDate,
            LocalDate endDate,
            String search
    );

    List<OrderResponse> getAllOrders();
}
