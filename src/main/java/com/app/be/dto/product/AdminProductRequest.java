package com.app.be.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class AdminProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String slug;

    private String categorySlug;

    @NotNull(message = "Giá cơ bản không được để trống")
    private BigDecimal basePrice;

    private String description;

    private String thumbnailUrl;

    private Boolean isActive;

    private List<AdminVariantRequest> variants;

    @Getter
    @Setter
    public static class AdminVariantRequest {
        private String id;
        private String sku;
        private String variantName;
        private BigDecimal price;
        private Integer stockQuantity;
    }
}
