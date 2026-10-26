package com.app.be.service;

import com.app.be.dto.common.PageResponse;
import com.app.be.dto.review.CreateReviewRequest;
import com.app.be.dto.review.ProductReviewSummaryResponse;
import com.app.be.dto.review.ReviewEligibilityResponse;
import com.app.be.dto.review.ReviewResponse;

import java.util.List;
import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(UUID userId, CreateReviewRequest request);

    ProductReviewSummaryResponse getReviewsByProductId(UUID productId, int page, int size);

    ProductReviewSummaryResponse getReviewsByProductSlug(String slug, int page, int size);

    ReviewEligibilityResponse checkEligibility(UUID userId, UUID productId);

    List<UUID> getReviewedProductIds(UUID userId);

    PageResponse<ReviewResponse> getMyReviews(UUID userId, int page, int size);
}
