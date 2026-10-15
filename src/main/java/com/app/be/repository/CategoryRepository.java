package com.app.be.repository;

import com.app.be.dto.category.CategoryResponse;
import com.app.be.entity.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Categories, UUID> {

    Optional<Categories> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Categories> findAllByOrderBySortOrderAsc();

    @Query("""
        SELECT new com.app.be.dto.category.CategoryResponse(
            c.id,
            c.name,
            c.slug,
            c.categoryTree,
            c.imageUrl,
            c.sortOrder,
            COUNT(p.id)
        )
        FROM Categories c
        LEFT JOIN c.products p ON p.isActive = true
        GROUP BY c.id, c.name, c.slug, c.categoryTree, c.imageUrl, c.sortOrder
        ORDER BY c.sortOrder ASC
    """)
    List<CategoryResponse> findAllWithProductCount();
}
