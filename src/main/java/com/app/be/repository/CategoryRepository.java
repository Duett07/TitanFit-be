package com.app.be.repository;

import com.app.be.entity.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Categories, UUID> {

    Optional<Categories> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Categories> findAllByOrderBySortOrderAsc();
}
