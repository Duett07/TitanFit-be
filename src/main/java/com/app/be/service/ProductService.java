package com.app.be.service;

import com.app.be.dto.common.PageResponse;
import com.app.be.dto.product.ProductDetailResponse;
import com.app.be.dto.product.ProductSummaryResponse;

import java.math.BigDecimal;

public interface ProductService {

    PageResponse<ProductSummaryResponse> getProducts(
            int page,
            int size,
            String keyword,
            String categorySlug,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy
    );

    ProductDetailResponse getProductBySlug(String slug);
}
