package com.app.be.dto.order;

import com.app.be.validation.VnPhone;
import jakarta.validation.constraints.NotBlank;
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
public class TrackOrderRequest {

    @NotBlank(message = "Mã đơn hàng (orderCode) không được để trống")
    private String orderCode;

    @NotBlank(message = "Số điện thoại (phone) không được để trống")
    @VnPhone
    private String phone;
}
