package com.app.be.controller.admin;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.OrderStatus;
import com.app.be.dto.order.UpdateOrderStatusRequest;
import com.app.be.exception.BadRequestException;
import com.app.be.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) UpdateOrderStatusRequest request,
            @RequestParam(name = "newStatus", required = false) OrderStatus paramStatus
    ) {
        OrderStatus targetStatus = null;
        if (request != null && request.getNewStatus() != null) {
            targetStatus = request.getNewStatus();
        } else if (paramStatus != null) {
            targetStatus = paramStatus;
        }

        if (targetStatus == null) {
            throw new BadRequestException("Trạng thái mới (newStatus) không được để trống (CONFIRMED, SHIPPING, COMPLETED, CANCELLED)");
        }

        OrderResponse response = orderService.updateOrderStatus(id, targetStatus);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", response));
    }
}
