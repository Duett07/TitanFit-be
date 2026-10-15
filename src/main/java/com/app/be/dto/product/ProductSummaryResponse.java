package com.app.be.dto.product;

import com.app.be.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {

    private UUID id;
    private String name;
    private String slug;
    private BigDecimal basePrice;
    private String thumbnailUrl;
    private String categoryName;
    private String categorySlug;
    private LocalDateTime createdAt;

    public static ProductSummaryResponse fromEntity(Product product) {
        if (product == null) {
            return null;
        }
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .basePrice(product.getBasePrice())
                .thumbnailUrl(product.getThumbnailUrl())
                .categoryName(product.getCategories() != null ? product.getCategories().getName() : null)
                .categorySlug(product.getCategories() != null ? product.getCategories().getSlug() : null)
                .createdAt(product.getCreatedAt())
                .build();
    }
}
