package com.app.be.controller.admin;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.OrderStatus;
import com.app.be.dto.order.PaymentStatus;
import com.app.be.dto.order.UpdateOrderStatusRequest;
import com.app.be.dto.order.UpdatePaymentStatusRequest;
import com.app.be.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> response = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tất cả đơn hàng thành công", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) UpdateOrderStatusRequest request,
            @RequestParam(name = "newStatus", required = false) OrderStatus paramStatus
    ) {
        OrderResponse response = orderService.updateOrderStatus(id, request, paramStatus);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", response));
    }

    @PatchMapping("/{id}/payment-status")
    public ResponseEntity<ApiResponse<OrderResponse>> updatePaymentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) UpdatePaymentStatusRequest request,
            @RequestParam(name = "paymentStatus", required = false) PaymentStatus paramStatus
    ) {
        OrderResponse response = orderService.updatePaymentStatus(id, request, paramStatus);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thanh toán thành công", response));
    }
}
