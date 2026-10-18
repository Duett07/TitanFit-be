package com.app.be.dto.order;

import com.app.be.enums.PaymentMethod;
import com.app.be.validation.VnPhone;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Tên người nhận (customerName) không được để trống")
    @Size(max = 255, message = "Tên người nhận không được vượt quá 255 ký tự")
    private String customerName;

    @NotBlank(message = "Số điện thoại nhận hàng (phone) không được để trống")
    @VnPhone
    private String phone;

    @NotBlank(message = "Địa chỉ nhận hàng (shippingAddress) không được để trống")
    @Size(max = 255, message = "Địa chỉ nhận hàng không được vượt quá 255 ký tự")
    private String shippingAddress;

    @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
    private String note;

    @NotNull(message = "Phương thức thanh toán (paymentMethod) không được để trống (COD hoặc VIETQR)")
    private PaymentMethod paymentMethod;

    @NotEmpty(message = "Danh sách mặt hàng không được để trống")
    @Valid
    @JsonAlias({"orderItems", "items"})
    private List<OrderItemRequest> items;
}
