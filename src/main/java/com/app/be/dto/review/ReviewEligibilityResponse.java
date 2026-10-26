package com.app.be.dto.review;

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
public class ReviewEligibilityResponse {

    private boolean eligible;
    private boolean hasPurchased;
    private boolean alreadyReviewed;
    private ReviewResponse userReview;
    private String message;
}
