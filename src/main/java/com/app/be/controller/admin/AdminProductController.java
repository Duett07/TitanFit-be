package com.app.be.controller.admin;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.common.PageResponse;
import com.app.be.dto.product.AdminProductRequest;
import com.app.be.dto.product.ProductDetailResponse;
import com.app.be.dto.product.ProductSummaryResponse;
import com.app.be.service.AdminProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Validated
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getAllProductsForAdmin(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Số trang (page) phải lớn hơn hoặc bằng 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Kích thước trang (size) tối thiểu là 1") @Max(value = 100, message = "Kích thước trang (size) tối đa là 100") int size,
            @RequestParam(required = false) @Size(max = 100, message = "Từ khóa tìm kiếm tối đa 100 ký tự") String keyword,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean isActive
    ) {
        Boolean targetActive = active != null ? active : isActive;
        PageResponse<ProductSummaryResponse> response = adminProductService.getAllProducts(
                page, size, keyword, categorySlug, targetActive
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy toàn bộ sản phẩm thành công", response));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<ProductSummaryResponse>> toggleActive(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean isActive
    ) {
        Boolean targetActive = active != null ? active : isActive;
        ProductSummaryResponse response = adminProductService.toggleActive(id, targetActive);
        return ResponseEntity.ok(ApiResponse.success("Đổi trạng thái sản phẩm thành công", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody AdminProductRequest request
    ) {
        ProductDetailResponse response = adminProductService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm mới sản phẩm thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody AdminProductRequest request
    ) {
        ProductDetailResponse response = adminProductService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        adminProductService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
    }
}
