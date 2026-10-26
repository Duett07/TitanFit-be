package com.app.be.controller;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.common.PageResponse;
import com.app.be.dto.review.CreateReviewRequest;
import com.app.be.dto.review.ProductReviewSummaryResponse;
import com.app.be.dto.review.ReviewEligibilityResponse;
import com.app.be.dto.review.ReviewResponse;
import com.app.be.exception.UnauthorizedException;
import com.app.be.security.user.CustomUserDetails;
import com.app.be.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để đánh giá");
        }
        ReviewResponse response = reviewService.createReview(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá sản phẩm thành công", response));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<ProductReviewSummaryResponse>> getReviewsByProductId(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Số trang phải >= 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Kích thước trang tối thiểu là 1") @Max(value = 50, message = "Kích thước trang tối đa là 50") int size
    ) {
        ProductReviewSummaryResponse response = reviewService.getReviewsByProductId(productId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", response));
    }

    @GetMapping("/product/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductReviewSummaryResponse>> getReviewsByProductSlug(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Số trang phải >= 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Kích thước trang tối thiểu là 1") @Max(value = 50, message = "Kích thước trang tối đa là 50") int size
    ) {
        ProductReviewSummaryResponse response = reviewService.getReviewsByProductSlug(slug, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", response));
    }

    @GetMapping("/eligibility/{productId}")
    public ResponseEntity<ApiResponse<ReviewEligibilityResponse>> checkEligibility(
            @PathVariable UUID productId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        ReviewEligibilityResponse response = reviewService.checkEligibility(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra điều kiện đánh giá thành công", response));
    }

    @GetMapping("/my-reviewed-product-ids")
    public ResponseEntity<ApiResponse<List<UUID>>> getMyReviewedProductIds(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập");
        }
        List<UUID> response = reviewService.getReviewedProductIds(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ID sản phẩm đã đánh giá thành công", response));
    }

    @GetMapping("/my-reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Số trang phải >= 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Kích thước trang tối thiểu là 1") @Max(value = 50, message = "Kích thước trang tối đa là 50") int size
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập");
        }
        PageResponse<ReviewResponse> response = reviewService.getMyReviews(userDetails.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá của bạn thành công", response));
    }
}
