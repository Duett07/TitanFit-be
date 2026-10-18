package com.app.be.service.impl;

import com.app.be.dto.product.AdminProductRequest;
import com.app.be.dto.product.ProductDetailResponse;
import com.app.be.dto.product.ProductSummaryResponse;
import com.app.be.entity.Categories;
import com.app.be.entity.Product;
import com.app.be.entity.ProductVariant;
import com.app.be.exception.BadRequestException;
import com.app.be.exception.ConflictException;
import com.app.be.exception.ResourceNotFoundException;
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

        Categories category = null;
        if (request.getCategorySlug() != null && !request.getCategorySlug().isBlank()) {
            String catSlug = request.getCategorySlug().trim();
            category = categoryRepository.findBySlug(catSlug)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với slug: " + catSlug));
        }

        Product product = new Product();
        product.setName(request.getName().trim());
        product.setSlug(slug);
        product.setBasePrice(request.getBasePrice());
        product.setDescription(request.getDescription());
        product.setThumbnailUrl(request.getThumbnailUrl());
        product.setActive(request.getIsActive() != null ? request.getIsActive() : true);
        product.setCategories(category);

        Product savedProduct = productRepository.save(product);

        // Validate trùng SKU trong chính request gửi lên
        validateVariantSkusInRequest(request.getVariants());

        List<ProductVariant> variants = new ArrayList<>();
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            for (int i = 0; i < request.getVariants().size(); i++) {
                AdminProductRequest.AdminVariantRequest vReq = request.getVariants().get(i);
                ProductVariant variant = new ProductVariant();
                variant.setProduct(savedProduct);
                variant.setVariantName(vReq.getVariantName() != null && !vReq.getVariantName().isBlank()
                        ? vReq.getVariantName().trim()
                        : "Tiêu chuẩn");
                BigDecimal variantPrice = (vReq.getPrice() != null && vReq.getPrice().compareTo(BigDecimal.ZERO) > 0)
                        ? vReq.getPrice()
                        : savedProduct.getBasePrice();
                variant.setPrice(variantPrice);
                variant.setStockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0);

                String sku = vReq.getSku();
                if (sku != null && !sku.isBlank()) {
                    sku = sku.trim().toUpperCase();
                    if (productVariantRepository.existsBySku(sku)) {
                        throw new ConflictException("Mã SKU '" + sku + "' đã tồn tại trên hệ thống, vui lòng chọn mã khác");
                    }
                } else {
                    sku = generateUniqueSku(savedProduct.getSlug(), i + 1);
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
            defVariant.setSku(generateUniqueSku(savedProduct.getSlug(), "DEF"));
            variants.add(defVariant);
        }

        try {
            List<ProductVariant> savedVariants = productVariantRepository.saveAll(variants);
            savedProduct.setProductVariants(savedVariants);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Lỗi toàn vẹn dữ liệu khi tạo biến thể sản phẩm: {}", ex.getMessage());
            throw new ConflictException("Lỗi dữ liệu: Mã SKU hoặc thông tin biến thể bị trùng lặp trên hệ thống");
        }

        log.info("Admin created new product: {} with {} variants", savedProduct.getName(), savedProduct.getProductVariants().size());
        return ProductDetailResponse.fromEntity(savedProduct);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(UUID id, AdminProductRequest request) {
        Product product = productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        // 1. Cập nhật và kiểm tra slug nếu admin gửi slug mới
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String newSlug = SlugUtils.generateSlug(request.getName(), request.getSlug());
            if (!newSlug.equalsIgnoreCase(product.getSlug())) {
                if (productRepository.existsBySlugAndIdNot(newSlug, product.getId())) {
                    throw new ConflictException("Slug '" + newSlug + "' đã tồn tại trên sản phẩm khác, vui lòng chọn slug khác");
                }
                product.setSlug(newSlug);
            }
        }

        // 2. Validate categorySlug: nếu gửi thì phải tìm thấy trong DB
        if (request.getCategorySlug() != null && !request.getCategorySlug().isBlank()) {
            String catSlug = request.getCategorySlug().trim();
            Categories category = categoryRepository.findBySlug(catSlug)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với slug: " + catSlug));
            product.setCategories(category);
        }

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

        Product savedProduct = productRepository.save(product);

        // 3. Quản lý vòng đời biến thể (Variants)
        if (request.getVariants() != null) {
            validateVariantSkusInRequest(request.getVariants());

            List<ProductVariant> existingVariants = productVariantRepository.findByProductId(savedProduct.getId());
            Map<UUID, ProductVariant> existingMap = new HashMap<>();
            for (ProductVariant ev : existingVariants) {
                if (ev.getId() != null) {
                    existingMap.put(ev.getId(), ev);
                }
            }

            Set<UUID> requestedVariantIds = new HashSet<>();
            for (AdminProductRequest.AdminVariantRequest vReq : request.getVariants()) {
                if (vReq.getId() != null && !vReq.getId().isBlank()) {
                    try {
                        requestedVariantIds.add(UUID.fromString(vReq.getId().trim()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            // Xóa các biến thể cũ không còn trong request nếu chưa từng có đơn hàng; nếu đã có đơn hàng thì chặn lại trả 409
            List<ProductVariant> variantsToDelete = new ArrayList<>();
            for (ProductVariant ev : existingVariants) {
                if (!requestedVariantIds.contains(ev.getId())) {
                    if (orderItemRepository.existsByProductVariantId(ev.getId())) {
                        throw new ConflictException("Không thể gỡ bỏ biến thể '" + ev.getVariantName() +
                                "' (SKU: " + ev.getSku() + ") do đã phát sinh đơn hàng trong hệ thống!");
                    }
                    variantsToDelete.add(ev);
                }
            }

            if (!variantsToDelete.isEmpty()) {
                productVariantRepository.deleteAll(variantsToDelete);
                productVariantRepository.flush();
            }

            List<ProductVariant> toSave = new ArrayList<>();
            for (int i = 0; i < request.getVariants().size(); i++) {
                AdminProductRequest.AdminVariantRequest vReq = request.getVariants().get(i);
                UUID variantId = null;
                if (vReq.getId() != null && !vReq.getId().isBlank()) {
                    try {
                        variantId = UUID.fromString(vReq.getId().trim());
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                ProductVariant variant;
                if (variantId != null && existingMap.containsKey(variantId)) {
                    variant = existingMap.get(variantId);
                } else {
                    variant = new ProductVariant();
                    variant.setProduct(savedProduct);
                }

                String sku = vReq.getSku();
                if (sku != null && !sku.isBlank()) {
                    sku = sku.trim().toUpperCase();
                    boolean skuExists = (variant.getId() != null)
                            ? productVariantRepository.existsBySkuAndIdNot(sku, variant.getId())
                            : productVariantRepository.existsBySku(sku);
                    if (skuExists) {
                        throw new ConflictException("Mã SKU '" + sku + "' đã tồn tại trên hệ thống, vui lòng chọn mã khác");
                    }
                } else {
                    if (variant.getSku() == null || variant.getSku().isBlank()) {
                        sku = generateUniqueSku(savedProduct.getSlug(), i + 1);
                    } else {
                        sku = variant.getSku();
                    }
                }
                variant.setSku(sku);

                if (vReq.getVariantName() != null && !vReq.getVariantName().isBlank()) {
                    variant.setVariantName(vReq.getVariantName().trim());
                } else if (variant.getVariantName() == null || variant.getVariantName().isBlank()) {
                    variant.setVariantName("Tiêu chuẩn");
                }

                if (vReq.getPrice() != null && vReq.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                    variant.setPrice(vReq.getPrice());
                } else if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    variant.setPrice(savedProduct.getBasePrice());
                }

                if (vReq.getStockQuantity() != null) {
                    variant.setStockQuantity(vReq.getStockQuantity());
                } else if (variant.getId() == null) {
                    variant.setStockQuantity(0);
                }

                toSave.add(variant);
            }

            try {
                List<ProductVariant> savedVariants = productVariantRepository.saveAll(toSave);
                savedProduct.setProductVariants(savedVariants);
            } catch (DataIntegrityViolationException ex) {
                log.warn("Lỗi toàn vẹn dữ liệu khi cập nhật biến thể sản phẩm {}: {}", id, ex.getMessage());
                throw new ConflictException("Lỗi dữ liệu: Mã SKU hoặc thông tin biến thể bị trùng lặp trên hệ thống");
            }
        }

        log.info("Admin updated product: {}", savedProduct.getName());
        return ProductDetailResponse.fromEntity(savedProduct);
    }

    private void validateVariantSkusInRequest(List<AdminProductRequest.AdminVariantRequest> variants) {
        if (variants == null || variants.isEmpty()) {
            return;
        }
        Set<String> seenSkus = new HashSet<>();
        for (AdminProductRequest.AdminVariantRequest vReq : variants) {
            if (vReq.getSku() != null && !vReq.getSku().isBlank()) {
                String normalizedSku = vReq.getSku().trim().toUpperCase();
                if (!seenSkus.add(normalizedSku)) {
                    throw new ConflictException("Danh sách biến thể chứa mã SKU trùng lặp: " + normalizedSku);
                }
            }
        }
    }

    private String generateUniqueSku(String slug, Object suffix) {
        String prefix = (slug != null ? slug : "PROD")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]", "");
        if (prefix.length() > 8) {
            prefix = prefix.substring(0, 8);
        }
        String sku = "SKU-" + prefix + "-" + suffix;
        while (productVariantRepository.existsBySku(sku)) {
            sku = "SKU-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        }
        return sku;
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
