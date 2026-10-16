package com.app.be.service;

import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.OrderStatus;
import com.app.be.dto.order.UpdateOrderStatusRequest;
import com.app.be.security.user.CustomUserDetails;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, CustomUserDetails userDetails);

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderByCode(String orderCode);

    List<OrderResponse> getMyOrders(CustomUserDetails userDetails);

    OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus);

    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request, OrderStatus paramStatus);

    List<OrderResponse> getAllOrders();
}
