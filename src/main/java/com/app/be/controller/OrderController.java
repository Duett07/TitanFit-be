package com.app.be.controller;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.TrackOrderRequest;
import com.app.be.dto.order.OrderResponse;
import com.app.be.security.user.CustomUserDetails;
import com.app.be.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        OrderResponse response = orderService.createOrder(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt hàng thành công", response));
    }

    
    @PostMapping("/track")
    public ResponseEntity<ApiResponse<OrderResponse>> trackOrder(
            @Valid @RequestBody TrackOrderRequest request
    ) {
        OrderResponse response = orderService.trackOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn hàng thành công", response));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<OrderResponse> response = orderService.getMyOrders(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", response));
    }

    @GetMapping("/{orderCode}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByCode(
            @PathVariable String orderCode,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        OrderResponse response = orderService.getOrderByCode(orderCode, userDetails);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn hàng thành công", response));
    }
}
