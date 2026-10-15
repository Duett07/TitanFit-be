package com.app.be.service;

import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.OrderResponse;
import com.app.be.security.user.CustomUserDetails;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, CustomUserDetails userDetails);

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderByCode(String orderCode);

    List<OrderResponse> getMyOrders(CustomUserDetails userDetails);
}
