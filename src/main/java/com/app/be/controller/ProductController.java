package com.app.be.controller;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.common.PageResponse;
import com.app.be.dto.product.ProductDetailResponse;
import com.app.be.dto.product.ProductSummaryResponse;
import com.app.be.service.ProductService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getProducts(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Số trang (page) phải lớn hơn hoặc bằng 0") int page,
            @RequestParam(defaultValue = "12") @Min(value = 1, message = "Kích thước trang (size) tối thiểu là 1") @Max(value = 100, message = "Kích thước trang (size) tối đa là 100") int size,
            @RequestParam(required = false) @Size(max = 100, message = "Từ khóa tìm kiếm tối đa 100 ký tự") String keyword,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sortBy
    ) {
        PageResponse<ProductSummaryResponse> response = productService.getProducts(
                page, size, keyword, categorySlug, minPrice, maxPrice, sortBy
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", response));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductBySlug(@PathVariable String slug) {
        ProductDetailResponse response = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết sản phẩm thành công", response));
    }
}
