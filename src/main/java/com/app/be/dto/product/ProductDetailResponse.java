package com.app.be.dto.product;

import com.app.be.dto.category.CategoryResponse;
import com.app.be.entity.Product;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private String thumbnailUrl;

    @JsonProperty("isActive")
    private boolean isActive;

    private CategoryResponse category;
    private List<ProductVariantResponse> variants;
    private LocalDateTime createdAt;

    @JsonProperty("isActive")
    public boolean isActive() {
        return isActive;
    }

    @JsonProperty("isActive")
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public static ProductDetailResponse fromEntity(Product product) {
        if (product == null) {
            return null;
        }

        CategoryResponse catResponse = null;
        if (product.getCategories() != null) {
            catResponse = CategoryResponse.builder()
                    .id(product.getCategories().getId())
                    .name(product.getCategories().getName())
                    .slug(product.getCategories().getSlug())
                    .categoryTree(product.getCategories().getCategoryTree())
                    .imageUrl(product.getCategories().getImageUrl())
                    .sortOrder(product.getCategories().getSortOrder())
                    .build();
        }

        List<ProductVariantResponse> variantResponses = product.getProductVariants() != null
                ? product.getProductVariants().stream()
                        .map(ProductVariantResponse::fromEntity)
                        .toList()
                : Collections.emptyList();

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .thumbnailUrl(product.getThumbnailUrl())
                .isActive(product.isActive())
                .category(catResponse)
                .variants(variantResponses)
                .createdAt(product.getCreatedAt())
                .build();
    }
}
