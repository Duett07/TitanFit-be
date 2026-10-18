package com.app.be.dto.order;

import com.app.be.enums.OrderStatus;
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
public class UpdateOrderStatusRequest {

    @NotNull(message = "Trạng thái mới (newStatus) không được để trống (CONFIRMED, SHIPPING, COMPLETED, CANCELLED)")
    @JsonAlias({"status", "newStatus"})
    private OrderStatus newStatus;
}
