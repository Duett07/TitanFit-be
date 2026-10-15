package com.app.be.controller;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.common.PageResponse;
import com.app.be.dto.product.ProductDetailResponse;
import com.app.be.dto.product.ProductSummaryResponse;
import com.app.be.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
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
