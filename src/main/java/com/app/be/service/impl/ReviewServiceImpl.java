package com.app.be.service.impl;

import com.app.be.dto.common.PageResponse;
import com.app.be.dto.review.CreateReviewRequest;
import com.app.be.dto.review.ProductReviewSummaryResponse;
import com.app.be.dto.review.ReviewEligibilityResponse;
import com.app.be.dto.review.ReviewResponse;
import com.app.be.entity.Product;
import com.app.be.entity.Review;
import com.app.be.entity.User;
import com.app.be.exception.BadRequestException;
import com.app.be.exception.ResourceNotFoundException;
import com.app.be.exception.UnauthorizedException;
import com.app.be.repository.OrderItemRepository;
import com.app.be.repository.ProductRepository;
import com.app.be.repository.ReviewRepository;
import com.app.be.repository.UserRepository;
import com.app.be.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request) {
        if (userId == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để đánh giá sản phẩm");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản người dùng không tồn tại hoặc đã bị khóa"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Sản phẩm hiện đang tạm ngừng kinh doanh, không thể đánh giá");
        }

        // 1. Kiểm tra xem người dùng đã từng đánh giá sản phẩm này chưa (chặn đánh giá trùng lặp)
        if (reviewRepository.existsByUser_IdAndProduct_Id(userId, request.getProductId())) {
            throw new BadRequestException("Bạn đã đánh giá sản phẩm này rồi. Mỗi khách hàng chỉ được đánh giá một lần.");
        }

        // 2. Kiểm tra điều kiện mua hàng: người dùng phải có đơn hàng COMPLETED chứa sản phẩm này
        boolean hasPurchased = orderItemRepository.hasUserPurchasedAndCompletedProduct(userId, request.getProductId());
        if (!hasPurchased) {
            throw new BadRequestException("Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và đơn hàng được giao thành công (COMPLETED).");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : "")
                .build();

        Review saved = reviewRepository.save(review);
        log.info("User {} reviewed product {} with rating: {}", userId, product.getId(), request.getRating());

        return ReviewResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewSummaryResponse getReviewsByProductId(UUID productId, int page, int size) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId);
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId, pageable);

        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        long total = reviewRepository.countByProduct_Id(productId);
        double averageRating = (avg != null && total > 0) ? Math.round(avg * 10.0) / 10.0 : 5.0;

        List<ReviewResponse> content = reviewPage.getContent().stream()
                .map(ReviewResponse::fromEntity)
                .toList();

        return ProductReviewSummaryResponse.builder()
                .averageRating(averageRating)
                .totalReviews(total)
                .reviews(PageResponse.from(reviewPage, content))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewSummaryResponse getReviewsByProductSlug(String slug, int page, int size) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với slug: " + slug));

        return getReviewsByProductId(product.getId(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse checkEligibility(UUID userId, UUID productId) {
        if (userId == null) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .hasPurchased(false)
                    .alreadyReviewed(false)
                    .message("Vui lòng đăng nhập để đánh giá sản phẩm")
                    .build();
        }

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId);
        }

        // Kiểm tra xem đã đánh giá chưa
        Optional<Review> existing = reviewRepository.findByUser_IdAndProduct_Id(userId, productId);
        if (existing.isPresent()) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .hasPurchased(true)
                    .alreadyReviewed(true)
                    .userReview(ReviewResponse.fromEntity(existing.get()))
                    .message("Bạn đã đánh giá sản phẩm này rồi")
                    .build();
        }

        // Kiểm tra xem đã mua và đơn hàng đã COMPLETED chưa
        boolean hasPurchased = orderItemRepository.hasUserPurchasedAndCompletedProduct(userId, productId);
        if (!hasPurchased) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .hasPurchased(false)
                    .alreadyReviewed(false)
                    .message("Bạn chỉ có thể đánh giá sau khi đã mua sản phẩm và đơn hàng đã hoàn tất (COMPLETED)")
                    .build();
        }

        return ReviewEligibilityResponse.builder()
                .eligible(true)
                .hasPurchased(true)
                .alreadyReviewed(false)
                .message("Bạn đủ điều kiện để đánh giá sản phẩm")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getReviewedProductIds(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return reviewRepository.findReviewedProductIdsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMyReviews(UUID userId, int page, int size) {
        if (userId == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);

        List<ReviewResponse> content = reviewPage.getContent().stream()
                .map(ReviewResponse::fromEntity)
                .toList();

        return PageResponse.from(reviewPage, content);
    }
}
