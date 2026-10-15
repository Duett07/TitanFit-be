package com.app.be.dto.order;

import com.app.be.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private UUID id;
    private UUID variantId;
    private String productName;
    private String variantName;
    private BigDecimal price;
    private int quantity;
    private BigDecimal totalPrice;

    public static OrderItemResponse fromEntity(OrderItem item) {
        if (item == null) {
            return null;
        }
        BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
        BigDecimal total = price.multiply(BigDecimal.valueOf(item.getQuantity()));

        return OrderItemResponse.builder()
                .id(item.getId())
                .variantId(item.getProductVariant() != null ? item.getProductVariant().getId() : null)
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .price(price)
                .quantity(item.getQuantity())
                .totalPrice(total)
                .build();
    }
}
