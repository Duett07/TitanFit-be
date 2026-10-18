package com.app.be.dto.order;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentStatusRequest {

    @NotNull(message = "Trạng thái thanh toán (paymentStatus) không được để trống (UNPAID, PAID, REFUNDED)")
    @JsonAlias({"status", "paymentStatus", "newStatus"})
    private PaymentStatus paymentStatus;
}
