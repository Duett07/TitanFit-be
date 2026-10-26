package com.app.be.repository;

import com.app.be.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @EntityGraph(attributePaths = {"user"})
    Page<Review> findByProduct_IdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<Review> findByProduct_IdOrderByCreatedAtDesc(UUID productId);

    boolean existsByUser_IdAndProduct_Id(UUID userId, UUID productId);

    Optional<Review> findByUser_IdAndProduct_Id(UUID userId, UUID productId);

    long countByProduct_Id(UUID productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") UUID productId);

    @Query("SELECT r.product.id FROM Review r WHERE r.user.id = :userId")
    List<UUID> findReviewedProductIdsByUserId(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"product"})
    Page<Review> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
