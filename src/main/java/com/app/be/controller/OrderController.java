package com.app.be.controller;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.common.PageResponse;
import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.TrackOrderRequest;
import com.app.be.enums.OrderStatus;
import com.app.be.enums.Role;
import com.app.be.security.user.CustomUserDetails;
import com.app.be.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        OrderResponse response = orderService.createOrder(request, userId);
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
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Số trang (page) phải lớn hơn hoặc bằng 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Kích thước trang (size) tối thiểu là 1") @Max(value = 100, message = "Kích thước trang (size) tối đa là 100") int size,
            @RequestParam(required = false) OrderStatus status
    ) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        PageResponse<OrderResponse> response = orderService.getMyOrders(userId, page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", response));
    }

    @GetMapping("/{orderCode}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByCode(
            @PathVariable String orderCode,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        boolean isAdmin = userDetails != null && userDetails.getAuthorities() != null && userDetails.getAuthorities().stream()
                .anyMatch(auth -> Role.ROLE_ADMIN.name().equalsIgnoreCase(auth.getAuthority()));
        OrderResponse response = orderService.getOrderByCode(orderCode, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn hàng thành công", response));
    }
}
