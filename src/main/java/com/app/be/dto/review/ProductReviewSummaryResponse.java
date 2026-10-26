package com.app.be.dto.review;

import com.app.be.dto.common.PageResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReviewSummaryResponse {

    private double averageRating;
    private long totalReviews;
    private PageResponse<ReviewResponse> reviews;
}
