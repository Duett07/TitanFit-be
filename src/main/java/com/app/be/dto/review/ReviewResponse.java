package com.app.be.dto.review;

import com.app.be.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private UUID userId;
    private String userName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) {
            return null;
        }

        String reviewerName = "Khách hàng TitanFit";
        if (review.getUser() != null && review.getUser().getFullName() != null && !review.getUser().getFullName().isBlank()) {
            reviewerName = review.getUser().getFullName();
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userName(reviewerName)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
