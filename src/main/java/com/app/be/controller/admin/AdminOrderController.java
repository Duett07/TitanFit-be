package com.app.be.controller.admin;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.common.PageResponse;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.UpdateOrderStatusRequest;
import com.app.be.dto.order.UpdatePaymentStatusRequest;
import com.app.be.enums.OrderStatus;
import com.app.be.enums.PaymentStatus;
import com.app.be.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Validated
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Số trang (page) phải lớn hơn hoặc bằng 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Kích thước trang (size) tối thiểu là 1") @Max(value = 100, message = "Kích thước trang (size) tối đa là 100") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @Size(max = 100, message = "Từ khóa tìm kiếm tối đa 100 ký tự") String search
    ) {
        PageResponse<OrderResponse> response = orderService.getAllOrders(
                page, size, status, paymentStatus, startDate, endDate, search
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tất cả đơn hàng thành công", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse response = orderService.updateOrderStatus(id, request.getNewStatus());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", response));
    }

    @PatchMapping("/{id}/payment-status")
    public ResponseEntity<ApiResponse<OrderResponse>> updatePaymentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        OrderResponse response = orderService.updatePaymentStatus(id, request.getPaymentStatus());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thanh toán thành công", response));
    }
}
