package com.app.be.config;

import com.app.be.entity.Categories;
import com.app.be.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class CategorySeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    private record SeedCategory(
            String name,
            String slug,
            String categoryTree,
            String imageUrl,
            int sortOrder
    ) {}

    private static final List<SeedCategory> DEFAULT_CATEGORIES = List.of(
            new SeedCategory(
                    "Whey Protein & Tăng Cơ",
                    "whey-protein",
                    "dinh-duong/whey-protein",
                    "https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?auto=format&fit=crop&w=600&q=80",
                    1
            ),
            new SeedCategory(
                    "BCAA & Amino Phục Hồi",
                    "bcaa-amino",
                    "dinh-duong/bcaa-amino",
                    "https://images.unsplash.com/photo-1593095948071-474c5cc2989d?auto=format&fit=crop&w=600&q=80",
                    2
            ),
            new SeedCategory(
                    "Pre-Workout & Tăng Năng Lượng",
                    "pre-workout",
                    "dinh-duong/pre-workout",
                    "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=600&q=80",
                    3
            ),
            new SeedCategory(
                    "Phụ Kiện & Dụng Cụ Gym",
                    "phu-kien-gym",
                    "phu-kien/phu-kien-gym",
                    "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&w=600&q=80",
                    4
            ),
            new SeedCategory(
                    "Quần Áo & Trang Phục Thể Thao",
                    "quan-ao-the-thao",
                    "thoi-trang/quan-ao-the-thao",
                    "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=600&q=80",
                    5
            ),
            new SeedCategory(
                    "Mass Gainer & Sữa Tăng Cân",
                    "mass-gainer",
                    "dinh-duong/mass-gainer",
                    "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80",
                    6
            ),
            new SeedCategory(
                    "Creatine Tăng Sức Mạnh",
                    "creatine",
                    "dinh-duong/creatine",
                    "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=600&q=80",
                    7
            ),
            new SeedCategory(
                    "Vitamin & Khoáng Chất Thiết Yếu",
                    "vitamin-khoang-chat",
                    "dinh-duong/vitamin-khoang-chat",
                    "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?auto=format&fit=crop&w=600&q=80",
                    8
            ),
            new SeedCategory(
                    "Bình Lắc & Shaker Thể Thao",
                    "binh-lac-shaker",
                    "phu-kien/binh-lac-shaker",
                    "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=600&q=80",
                    9
            ),
            new SeedCategory(
                    "Bánh Protein & Snack Healthy",
                    "snack-protein-healthy",
                    "dinh-duong/snack-protein-healthy",
                    "https://images.unsplash.com/photo-1622484214149-68d0e52731bc?auto=format&fit=crop&w=600&q=80",
                    10
            )
    );

    @Override
    @Transactional
    public void run(String... args) {
        seedCategories();
    }

    private void seedCategories() {
        for (SeedCategory seed : DEFAULT_CATEGORIES) {
            if (!categoryRepository.existsBySlug(seed.slug())) {
                Categories category = new Categories();
                category.setName(seed.name());
                category.setSlug(seed.slug());
                category.setCategoryTree(seed.categoryTree());
                category.setImageUrl(seed.imageUrl());
                category.setSortOrder(seed.sortOrder());

                categoryRepository.save(category);
                log.info("Initialized default category: {} ({})", seed.name(), seed.slug());
            } else {
                log.debug("Category already exists with slug: {}", seed.slug());
            }
        }
    }
}
