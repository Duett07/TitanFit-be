package com.app.be.service;

import com.app.be.dto.common.PageResponse;
import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.TrackOrderRequest;
import com.app.be.enums.OrderStatus;
import com.app.be.enums.PaymentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, UUID userId);

    OrderResponse getOrderByCode(String orderCode, UUID userId, boolean isAdmin);

    OrderResponse trackOrder(TrackOrderRequest request);

    PageResponse<OrderResponse> getMyOrders(
            UUID userId,
            int page,
            int size,
            OrderStatus status
    );

    List<OrderResponse> getMyOrders(UUID userId);

    OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus);

    OrderResponse updatePaymentStatus(UUID orderId, PaymentStatus newStatus);

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
