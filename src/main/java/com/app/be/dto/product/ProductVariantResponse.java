package com.app.be.dto.product;

import com.app.be.entity.ProductVariant;
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
public class ProductVariantResponse {

    private UUID id;
    private String sku;
    private String variantName;
    private BigDecimal price;
    private int stockQuantity;

    public static ProductVariantResponse fromEntity(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .variantName(variant.getVariantName())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .build();
    }
}
