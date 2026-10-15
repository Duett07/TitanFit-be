package com.app.be.repository;

import com.app.be.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Page<Product> findByCategories_Id(UUID categoryId, Pageable pageable);

    Page<Product> findByCategories_IdAndIsActiveTrue(UUID categoryId, Pageable pageable);

    Page<Product> findByCategories_Slug(String slug, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"categories", "productVariants"})
    Optional<Product> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"categories", "productVariants"})
    Optional<Product> findWithDetailsBySlug(String slug);
}
