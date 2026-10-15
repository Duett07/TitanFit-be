package com.app.be.service.impl;

import com.app.be.dto.common.PageResponse;
import com.app.be.dto.product.ProductDetailResponse;
import com.app.be.dto.product.ProductSummaryResponse;
import com.app.be.entity.Product;
import com.app.be.exception.ResourceNotFoundException;
import com.app.be.repository.ProductRepository;
import com.app.be.repository.specification.ProductSpecification;
import com.app.be.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getProducts(
            int page,
            int size,
            String keyword,
            String categorySlug,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy
    ) {
        Sort sort = resolveSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Product> spec = ProductSpecification.filterProducts(
                keyword, categorySlug, minPrice, maxPrice
        );

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<ProductSummaryResponse> content = productPage.getContent().stream()
                .map(ProductSummaryResponse::fromEntity)
                .toList();

        return PageResponse.from(productPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findWithDetailsBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với slug: " + slug));

        if (!product.isActive()) {
            throw new ResourceNotFoundException("Sản phẩm hiện không khả dụng hoặc đã ngừng kinh doanh");
        }

        return ProductDetailResponse.fromEntity(product);
    }

    private Sort resolveSort(String sortBy) {
        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return switch (sortBy.trim().toLowerCase()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "basePrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "basePrice");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
