package com.app.be.controller.admin;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.product.AdminProductRequest;
import com.app.be.dto.product.ProductDetailResponse;
import com.app.be.dto.product.ProductSummaryResponse;
import com.app.be.service.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getAllProductsForAdmin() {
        List<ProductSummaryResponse> response = adminProductService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success("Lấy toàn bộ sản phẩm thành công", response));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<ProductSummaryResponse>> toggleActive(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean active
    ) {
        ProductSummaryResponse response = adminProductService.toggleActive(id, active);
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
