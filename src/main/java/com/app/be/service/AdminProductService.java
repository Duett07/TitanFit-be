package com.app.be.service;

import com.app.be.dto.common.PageResponse;
import com.app.be.dto.product.AdminProductRequest;
import com.app.be.dto.product.ProductDetailResponse;
import com.app.be.dto.product.ProductSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface AdminProductService {

    PageResponse<ProductSummaryResponse> getAllProducts(
            int page,
            int size,
            String keyword,
            String categorySlug,
            Boolean isActive
    );

    List<ProductSummaryResponse> getAllProducts();

    ProductSummaryResponse toggleActive(UUID id, Boolean active);

    ProductDetailResponse createProduct(AdminProductRequest request);

    ProductDetailResponse updateProduct(UUID id, AdminProductRequest request);

    void deleteProduct(UUID id);
}
