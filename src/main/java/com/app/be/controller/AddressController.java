package com.app.be.controller;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.address.AddressRequest;
import com.app.be.dto.address.AddressResponse;
import com.app.be.exception.UnauthorizedException;
import com.app.be.security.user.CustomUserDetails;
import com.app.be.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Chưa đăng nhập");
        }
        List<AddressResponse> result = addressService.getMyAddresses(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách địa chỉ thành công", result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddressRequest request
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Chưa đăng nhập");
        }
        AddressResponse result = addressService.createAddress(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm địa chỉ mới thành công", result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Chưa đăng nhập");
        }
        AddressResponse result = addressService.updateAddress(userDetails.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ thành công", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Chưa đăng nhập");
        }
        addressService.deleteAddress(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Xóa địa chỉ thành công", null));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Chưa đăng nhập");
        }
        AddressResponse result = addressService.setDefaultAddress(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã đặt làm địa chỉ giao hàng mặc định", result));
    }
}
