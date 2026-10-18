package com.app.be.service.impl;

import com.app.be.dto.product.AdminProductRequest;
import com.app.be.dto.product.ProductDetailResponse;
import com.app.be.dto.product.ProductSummaryResponse;
import com.app.be.entity.Product;
import com.app.be.entity.ProductVariant;
import com.app.be.exception.ResourceNotFoundException;
import com.app.be.exception.ConflictException;
import com.app.be.repository.CategoryRepository;
import com.app.be.repository.OrderItemRepository;
import com.app.be.repository.ProductRepository;
import com.app.be.repository.ProductVariantRepository;
import com.app.be.service.AdminProductService;
import com.app.be.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getAllProducts() {
        List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return products.stream()
                .map(ProductSummaryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public ProductSummaryResponse toggleActive(UUID id, Boolean active) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        boolean newActive = (active != null) ? active : !product.isActive();
        product.setActive(newActive);
        Product saved = productRepository.save(product);

        log.info("Admin changed product '{}' isActive to {}", saved.getName(), newActive);
        return ProductSummaryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ProductDetailResponse createProduct(AdminProductRequest request) {
        String slug = SlugUtils.generateSlug(request.getName(), request.getSlug());
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
                BigDecimal variantPrice = (vReq.getPrice() != null && vReq.getPrice().compareTo(BigDecimal.ZERO) > 0)
                        ? vReq.getPrice()
                        : savedProduct.getBasePrice();
                variant.setPrice(variantPrice);
                variant.setStockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0);

                String sku = vReq.getSku();
                if (sku == null || sku.isBlank()) {
                    String prefix = savedProduct.getSlug().toUpperCase().replace("-", "");
                    sku = "SKU-" + prefix.substring(0, Math.min(8, prefix.length())) + "-" + (variants.size() + 1);
                }
                while (productVariantRepository.existsBySku(sku)) {
                    sku = sku + "-" + (int) (Math.random() * 900 + 100);
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
        return ProductDetailResponse.fromEntity(savedProduct);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(UUID id, AdminProductRequest request) {
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
                if (vReq.getPrice() != null && vReq.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                    variant.setPrice(vReq.getPrice());
                } else if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    variant.setPrice(savedProduct.getBasePrice());
                }
                if (vReq.getStockQuantity() != null) variant.setStockQuantity(vReq.getStockQuantity());
                toSave.add(variant);
            }

            List<ProductVariant> savedVariants = productVariantRepository.saveAll(toSave);
            savedProduct.setProductVariants(savedVariants);
        }

        log.info("Admin updated product: {}", savedProduct.getName());
        return ProductDetailResponse.fromEntity(savedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        // Kiểm tra ràng buộc: Nếu sản phẩm đã từng phát sinh đơn hàng -> Chặn xóa và trả lỗi 409 Conflict
        if (orderItemRepository.existsByProductVariantProductId(id)) {
            throw new ConflictException(
                    "Sản phẩm '" + product.getName() + "' đã có đơn hàng trong hệ thống, không thể xóa vĩnh viễn. Vui lòng tắt kích hoạt (ẩn sản phẩm) để ngừng kinh doanh!"
            );
        }

        try {
            List<ProductVariant> variants = productVariantRepository.findByProductId(id);
            productVariantRepository.deleteAll(variants);
            productRepository.delete(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            log.warn("Không thể xóa sản phẩm ID {} do ràng buộc cơ sở dữ liệu: {}", id, ex.getMessage());
            throw new ConflictException(
                    "Không thể xóa sản phẩm '" + product.getName() + "' do tồn tại dữ liệu liên kết. Vui lòng ẩn sản phẩm thay vì xóa!"
            );
        }

        log.info("Admin deleted product: {}", product.getName());
    }
}
