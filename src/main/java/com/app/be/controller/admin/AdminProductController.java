package com.app.be.controller.admin;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.product.AdminProductRequest;
import com.app.be.dto.product.ProductDetailResponse;
import com.app.be.dto.product.ProductSummaryResponse;
import com.app.be.entity.Product;
import com.app.be.entity.ProductVariant;
import com.app.be.exception.ResourceNotFoundException;
import com.app.be.repository.CategoryRepository;
import com.app.be.repository.ProductRepository;
import com.app.be.repository.ProductVariantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getAllProductsForAdmin() {
        List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ProductSummaryResponse> response = products.stream()
                .map(ProductSummaryResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy toàn bộ sản phẩm thành công", response));
    }

    @PatchMapping("/{id}/toggle-active")
    @Transactional
    public ResponseEntity<ApiResponse<ProductSummaryResponse>> toggleActive(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean active
    ) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        boolean newActive = (active != null) ? active : !product.isActive();
        product.setActive(newActive);
        Product saved = productRepository.save(product);

        log.info("Admin changed product '{}' isActive to {}", saved.getName(), newActive);
        return ResponseEntity.ok(ApiResponse.success("Đổi trạng thái sản phẩm thành công", ProductSummaryResponse.fromEntity(saved)));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody AdminProductRequest request
    ) {
        String slug = generateSlug(request.getName(), request.getSlug());
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 5);
        }

        Product product = new Product();
        product.setName(request.getName().trim());
        product.setSlug(slug);
        product.setBasePrice(request.getBasePrice());
        product.setDescription(request.getDescription());
        product.setThumbnailUrl(request.getThumbnailUrl());
        product.setActive(request.getIsActive() != null ? request.getIsActive() : true);

        if (request.getCategorySlug() != null && !request.getCategorySlug().isBlank()) {
            categoryRepository.findBySlug(request.getCategorySlug().trim())
                    .ifPresent(product::setCategories);
        }

        Product savedProduct = productRepository.save(product);

        List<ProductVariant> variants = new ArrayList<>();
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            for (AdminProductRequest.AdminVariantRequest vReq : request.getVariants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(savedProduct);
                variant.setVariantName(vReq.getVariantName() != null ? vReq.getVariantName().trim() : "Tiêu chuẩn");
                variant.setPrice(vReq.getPrice() != null ? vReq.getPrice() : savedProduct.getBasePrice());
                variant.setStockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0);

                String sku = vReq.getSku();
                if (sku == null || sku.isBlank()) {
                    String prefix = savedProduct.getSlug().toUpperCase().replace("-", "");
                    sku = "SKU-" + prefix.substring(0, Math.min(8, prefix.length())) + "-" + (variants.size() + 1);
                }
                while (productVariantRepository.existsBySku(sku)) {
                    sku = sku + "-" + (int)(Math.random() * 900 + 100);
                }
                variant.setSku(sku);
                variants.add(variant);
            }
        } else {
            ProductVariant defVariant = new ProductVariant();
            defVariant.setProduct(savedProduct);
            defVariant.setVariantName("Mặc định");
            defVariant.setPrice(savedProduct.getBasePrice());
            defVariant.setStockQuantity(50);
            String prefix = savedProduct.getSlug().toUpperCase().replace("-", "");
            defVariant.setSku("SKU-" + prefix.substring(0, Math.min(8, prefix.length())) + "-DEF");
            variants.add(defVariant);
        }

        List<ProductVariant> savedVariants = productVariantRepository.saveAll(variants);
        savedProduct.setProductVariants(savedVariants);

        log.info("Admin created new product: {} with {} variants", savedProduct.getName(), savedVariants.size());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm mới sản phẩm thành công", ProductDetailResponse.fromEntity(savedProduct)));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody AdminProductRequest request
    ) {
        Product product = productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        product.setName(request.getName().trim());
        product.setBasePrice(request.getBasePrice());
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getThumbnailUrl() != null && !request.getThumbnailUrl().isBlank()) {
            product.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getIsActive() != null) {
            product.setActive(request.getIsActive());
        }

        if (request.getCategorySlug() != null && !request.getCategorySlug().isBlank()) {
            categoryRepository.findBySlug(request.getCategorySlug().trim())
                    .ifPresent(product::setCategories);
        }

        Product savedProduct = productRepository.save(product);

        if (request.getVariants() != null) {
            List<ProductVariant> existingVariants = productVariantRepository.findByProductId(savedProduct.getId());
            Map<String, ProductVariant> existingMap = new HashMap<>();
            for (ProductVariant ev : existingVariants) {
                if (ev.getId() != null) {
                    existingMap.put(ev.getId().toString(), ev);
                }
            }

            List<ProductVariant> toSave = new ArrayList<>();
            for (AdminProductRequest.AdminVariantRequest vReq : request.getVariants()) {
                ProductVariant variant;
                if (vReq.getId() != null && existingMap.containsKey(vReq.getId())) {
                    variant = existingMap.get(vReq.getId());
                } else {
                    variant = new ProductVariant();
                    variant.setProduct(savedProduct);
                    String sku = vReq.getSku();
                    if (sku == null || sku.isBlank()) {
                        sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    }
                    variant.setSku(sku);
                }

                if (vReq.getVariantName() != null) variant.setVariantName(vReq.getVariantName().trim());
                if (vReq.getPrice() != null) variant.setPrice(vReq.getPrice());
                if (vReq.getStockQuantity() != null) variant.setStockQuantity(vReq.getStockQuantity());
                toSave.add(variant);
            }

            List<ProductVariant> savedVariants = productVariantRepository.saveAll(toSave);
            savedProduct.setProductVariants(savedVariants);
        }

        log.info("Admin updated product: {}", savedProduct.getName());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", ProductDetailResponse.fromEntity(savedProduct)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        List<ProductVariant> variants = productVariantRepository.findByProductId(id);
        productVariantRepository.deleteAll(variants);
        productRepository.delete(product);

        log.info("Admin deleted product: {}", product.getName());
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
    }

    private String generateSlug(String name, String customSlug) {
        if (customSlug != null && !customSlug.isBlank()) {
            return toSlug(customSlug);
        }
        return toSlug(name);
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("[\\s]").matcher(input.trim().toLowerCase()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        slug = slug.replaceAll("[đĐ]", "d");
        slug = Pattern.compile("[^a-z0-9-]").matcher(slug).replaceAll("");
        slug = Pattern.compile("-+").matcher(slug).replaceAll("-");
        return slug.replaceAll("^-|-$", "");
    }
}
