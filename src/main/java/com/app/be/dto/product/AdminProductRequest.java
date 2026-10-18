package com.app.be.dto.product;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class AdminProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 255, message = "Slug không được vượt quá 255 ký tự")
    private String slug;

    @Size(max = 255, message = "Category slug không được vượt quá 255 ký tự")
    private String categorySlug;

    @NotNull(message = "Giá cơ bản không được để trống")
    @PositiveOrZero(message = "Giá cơ bản phải lớn hơn hoặc bằng 0")
    private BigDecimal basePrice;

    private String description;

    @Size(max = 500, message = "URL hình ảnh không được vượt quá 500 ký tự")
    @Pattern(regexp = "^(https?://.+)?$", message = "URL hình ảnh phải có định dạng hợp lệ bắt đầu bằng http:// hoặc https://")
    private String thumbnailUrl;

    @JsonProperty("isActive")
    @JsonAlias({"active", "isActive"})
    private Boolean isActive;

    @JsonProperty("isActive")
    public Boolean getIsActive() {
        return isActive;
    }

    @JsonProperty("isActive")
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Valid
    private List<AdminVariantRequest> variants;

    @Getter
    @Setter
    public static class AdminVariantRequest {

        @Size(max = 36, message = "ID biến thể không được vượt quá 36 ký tự")
        private String id;

        @Size(max = 100, message = "Mã SKU không được vượt quá 100 ký tự")
        private String sku;

        @Size(max = 255, message = "Tên biến thể không được vượt quá 255 ký tự")
        private String variantName;

        @PositiveOrZero(message = "Giá biến thể phải lớn hơn hoặc bằng 0")
        private BigDecimal price;

        @Min(value = 0, message = "Số lượng tồn kho không được âm")
        @Max(value = 1000000, message = "Số lượng tồn kho không được vượt quá 1,000,000")
        private Integer stockQuantity;
    }
}

