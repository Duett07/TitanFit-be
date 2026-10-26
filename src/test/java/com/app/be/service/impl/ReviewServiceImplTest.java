package com.app.be.service.impl;

import com.app.be.dto.review.CreateReviewRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for ReviewServiceImpl")
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private UUID userId;
    private UUID productId;
    private User mockUser;
    private Product mockProduct;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        mockUser = new User();
        mockUser.setId(userId);
        mockUser.setFullName("Nguyen Van A");

        mockProduct = new Product();
        mockProduct.setId(productId);
        mockProduct.setName("Whey Gold Standard");
        mockProduct.setActive(true);
    }

    @Nested
    @DisplayName("Tests for createReview()")
    class CreateReviewTests {

        @Test
        @DisplayName("Throw UnauthorizedException if userId is null")
        void whenUserIdNull_thenThrowUnauthorized() {
            CreateReviewRequest req = CreateReviewRequest.builder()
                    .productId(productId)
                    .rating(5)
                    .comment("Sản phẩm tốt")
                    .build();

            assertThatThrownBy(() -> reviewService.createReview(null, req))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Vui lòng đăng nhập");
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException if product not found")
        void whenProductNotFound_thenThrowResourceNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            CreateReviewRequest req = CreateReviewRequest.builder()
                    .productId(productId)
                    .rating(5)
                    .build();

            assertThatThrownBy(() -> reviewService.createReview(userId, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Throw BadRequestException if user already reviewed this product")
        void whenAlreadyReviewed_thenThrowBadRequest() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
            when(reviewRepository.existsByUser_IdAndProduct_Id(userId, productId)).thenReturn(true);

            CreateReviewRequest req = CreateReviewRequest.builder()
                    .productId(productId)
                    .rating(5)
                    .build();

            assertThatThrownBy(() -> reviewService.createReview(userId, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Bạn đã đánh giá sản phẩm này rồi");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throw BadRequestException if user has not purchased product in COMPLETED order")
        void whenNotPurchasedInCompletedOrder_thenThrowBadRequest() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
            when(reviewRepository.existsByUser_IdAndProduct_Id(userId, productId)).thenReturn(false);
            when(orderItemRepository.hasUserPurchasedAndCompletedProduct(userId, productId)).thenReturn(false);

            CreateReviewRequest req = CreateReviewRequest.builder()
                    .productId(productId)
                    .rating(5)
                    .comment("Chưa mua nhưng muốn đánh giá")
                    .build();

            assertThatThrownBy(() -> reviewService.createReview(userId, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("đơn hàng được giao thành công (COMPLETED)");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Save and return ReviewResponse when user has completed order and hasn't reviewed")
        void whenEligible_thenSaveAndReturnReview() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
            when(reviewRepository.existsByUser_IdAndProduct_Id(userId, productId)).thenReturn(false);
            when(orderItemRepository.hasUserPurchasedAndCompletedProduct(userId, productId)).thenReturn(true);

            Review savedReview = Review.builder()
                    .user(mockUser)
                    .product(mockProduct)
                    .rating(5)
                    .comment("Sản phẩm rất tốt!")
                    .build();
            savedReview.setId(UUID.randomUUID());

            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            CreateReviewRequest req = CreateReviewRequest.builder()
                    .productId(productId)
                    .rating(5)
                    .comment("Sản phẩm rất tốt!")
                    .build();

            ReviewResponse response = reviewService.createReview(userId, req);

            assertThat(response).isNotNull();
            assertThat(response.getRating()).isEqualTo(5);
            assertThat(response.getComment()).isEqualTo("Sản phẩm rất tốt!");
            assertThat(response.getUserName()).isEqualTo("Nguyen Van A");
            verify(reviewRepository, times(1)).save(any(Review.class));
        }
    }

    @Nested
    @DisplayName("Tests for checkEligibility()")
    class CheckEligibilityTests {

        @Test
        @DisplayName("Return not eligible when userId is null")
        void whenUserIdNull_thenReturnNotEligible() {
            ReviewEligibilityResponse res = reviewService.checkEligibility(null, productId);
            assertThat(res.isEligible()).isFalse();
            assertThat(res.getMessage()).contains("Vui lòng đăng nhập");
        }

        @Test
        @DisplayName("Return already reviewed when user already has review")
        void whenAlreadyReviewed_thenReturnAlreadyReviewed() {
            when(productRepository.existsById(productId)).thenReturn(true);
            Review existingReview = Review.builder()
                    .user(mockUser)
                    .product(mockProduct)
                    .rating(4)
                    .comment("Ổn")
                    .build();
            existingReview.setId(UUID.randomUUID());

            when(reviewRepository.findByUser_IdAndProduct_Id(userId, productId))
                    .thenReturn(Optional.of(existingReview));

            ReviewEligibilityResponse res = reviewService.checkEligibility(userId, productId);
            assertThat(res.isEligible()).isFalse();
            assertThat(res.isAlreadyReviewed()).isTrue();
            assertThat(res.getUserReview()).isNotNull();
            assertThat(res.getUserReview().getRating()).isEqualTo(4);
        }

        @Test
        @DisplayName("Return not eligible when user has not purchased in COMPLETED order")
        void whenNotPurchased_thenReturnNotEligible() {
            when(productRepository.existsById(productId)).thenReturn(true);
            when(reviewRepository.findByUser_IdAndProduct_Id(userId, productId)).thenReturn(Optional.empty());
            when(orderItemRepository.hasUserPurchasedAndCompletedProduct(userId, productId)).thenReturn(false);

            ReviewEligibilityResponse res = reviewService.checkEligibility(userId, productId);
            assertThat(res.isEligible()).isFalse();
            assertThat(res.isAlreadyReviewed()).isFalse();
            assertThat(res.isHasPurchased()).isFalse();
            assertThat(res.getMessage()).contains("COMPLETED");
        }

        @Test
        @DisplayName("Return eligible when user has purchased in COMPLETED order and not reviewed yet")
        void whenEligible_thenReturnEligible() {
            when(productRepository.existsById(productId)).thenReturn(true);
            when(reviewRepository.findByUser_IdAndProduct_Id(userId, productId)).thenReturn(Optional.empty());
            when(orderItemRepository.hasUserPurchasedAndCompletedProduct(userId, productId)).thenReturn(true);

            ReviewEligibilityResponse res = reviewService.checkEligibility(userId, productId);
            assertThat(res.isEligible()).isTrue();
            assertThat(res.isAlreadyReviewed()).isFalse();
            assertThat(res.isHasPurchased()).isTrue();
        }
    }
}
