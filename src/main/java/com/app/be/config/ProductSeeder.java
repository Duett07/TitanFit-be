package com.app.be.config;

import com.app.be.entity.Categories;
import com.app.be.entity.Product;
import com.app.be.entity.ProductVariant;
import com.app.be.repository.CategoryRepository;
import com.app.be.repository.ProductRepository;
import com.app.be.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class ProductSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;

    private record SeedVariant(
            String sku,
            String variantName,
            BigDecimal price,
            int stockQuantity
    ) {}

    private record SeedProduct(
            String name,
            String slug,
            String description,
            BigDecimal basePrice,
            String thumbnailUrl,
            String categorySlug,
            List<SeedVariant> variants
    ) {}

    private static final List<SeedProduct> DEFAULT_PRODUCTS = List.of(
            // 1. Whey Gold Standard 100%
            new SeedProduct(
                    "Sữa Tăng Cơ Optimum Nutrition Whey Gold Standard 100%",
                    "whey-gold-standard-100",
                    "Whey Gold Standard là dòng sản phẩm whey protein bán chạy số 1 thế giới với sự kết hợp tối ưu giữa Whey Isolate, Whey Concentrate và Hydrolyzed Whey Peptides giúp phục hồi và phát triển cơ bắp vượt trội.",
                    BigDecimal.valueOf(1550000),
                    "https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?auto=format&fit=crop&w=600&q=80",
                    "whey-protein",
                    List.of(
                            new SeedVariant("WHEY-GOLD-CHOCO-5LBS", "Vị Double Rich Chocolate (5 Lbs / 2.27kg)", BigDecimal.valueOf(1550000), 50),
                            new SeedVariant("WHEY-GOLD-VANILLA-5LBS", "Vị Vanilla Ice Cream (5 Lbs / 2.27kg)", BigDecimal.valueOf(1550000), 35),
                            new SeedVariant("WHEY-GOLD-STRAWBERRY-5LBS", "Vị Delicious Strawberry (5 Lbs / 2.27kg)", BigDecimal.valueOf(1590000), 20)
                    )
            ),

            // 2. Rule 1 Protein Isolate
            new SeedProduct(
                    "Sữa Tăng Cơ Rule 1 Proteins R1 Protein Isolate",
                    "rule-1-protein-isolate",
                    "R1 Protein với thành phần 100% Whey Protein Isolate & Hydrolysate siêu tinh khiết, hầu như không chứa đường, fat, gluten hay chất phụ gia, hỗ trợ hấp thu cực nhanh sau tập luyện.",
                    BigDecimal.valueOf(1650000),
                    "https://images.unsplash.com/photo-1593095948071-474c5cc2989d?auto=format&fit=crop&w=600&q=80",
                    "whey-protein",
                    List.of(
                            new SeedVariant("R1-ISOLATE-CHOCO-FUDGE-5LBS", "Vị Chocolate Fudge (5 Lbs / 2.23kg)", BigDecimal.valueOf(1650000), 45),
                            new SeedVariant("R1-ISOLATE-CAFE-MOCHA-5LBS", "Vị Cafe Mocha (5 Lbs / 2.23kg)", BigDecimal.valueOf(1690000), 30)
                    )
            ),

            // 3. Pre-Workout ABE Applied Nutrition
            new SeedProduct(
                    "Tăng Năng Lượng Applied Nutrition ABE Pre-Workout 315g",
                    "applied-nutrition-abe-pre-workout",
                    "ABE Pre-Workout mang lại nguồn năng lượng bùng nổ, tăng sức mạnh và khả năng tập trung cao độ, đẩy giới hạn bài tập lên mức tối đa với TeaCrine, Citrulline Malate và Beta-Alanine.",
                    BigDecimal.valueOf(650000),
                    "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=600&q=80",
                    "pre-workout",
                    List.of(
                            new SeedVariant("ABE-PRE-ENERGY-FLAVOR-315G", "Vị Energy Flavor (30 lần dùng)", BigDecimal.valueOf(650000), 60),
                            new SeedVariant("ABE-PRE-SOUR-APPLE-315G", "Vị Táo Chua Sour Apple (30 lần dùng)", BigDecimal.valueOf(650000), 40),
                            new SeedVariant("ABE-PRE-TROPICAL-315G", "Vị Tropical Crush (30 lần dùng)", BigDecimal.valueOf(650000), 30)
                    )
            ),

            // 4. Pre-Workout Cellucor C4 Original
            new SeedProduct(
                    "Tăng Sức Mạnh Cellucor C4 Original 30 Servings",
                    "cellucor-c4-original-pre-workout",
                    "C4 Original huyền thoại của thương hiệu Cellucor mang lại năng lượng bền bỉ, cảm giác bơm cơ ấn tượng và tăng sức bền cơ bắp nhờ CarnoSyn Beta-Alanine và Arginine AKG.",
                    BigDecimal.valueOf(680000),
                    "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=600&q=80",
                    "pre-workout",
                    List.of(
                            new SeedVariant("C4-ORIGINAL-ICY-BLUE-30S", "Vị Icy Blue Razz (30 lần dùng)", BigDecimal.valueOf(680000), 45),
                            new SeedVariant("C4-ORIGINAL-FRUIT-PUNCH-30S", "Vị Fruit Punch (30 lần dùng)", BigDecimal.valueOf(680000), 50)
                    )
            ),

            // 5. Scivation Xtend BCAA
            new SeedProduct(
                    "Phục Hồi Cơ Bắp Scivation Xtend BCAA 30 Servings",
                    "scivation-xtend-bcaa-30-servings",
                    "Xtend BCAA chứa tỷ lệ vàng 2:1:1 (Leucine, Isoleucine, Valine) kết hợp chất điện giải Electrolyte giúp chống dị hóa cơ bắp, bù nước và giảm đau mỏi cơ bắp trong và sau khi tập.",
                    BigDecimal.valueOf(580000),
                    "https://images.unsplash.com/photo-1593095948071-474c5cc2989d?auto=format&fit=crop&w=600&q=80",
                    "bcaa-amino",
                    List.of(
                            new SeedVariant("XTEND-BCAA-WATERMELON-30S", "Vị Dưa Hấu Watermelon (30 servings)", BigDecimal.valueOf(580000), 75),
                            new SeedVariant("XTEND-BCAA-MANGO-30S", "Vị Xoài Mango Madness (30 servings)", BigDecimal.valueOf(580000), 60),
                            new SeedVariant("XTEND-BCAA-GRAPE-30S", "Vị Nho Glacial Grape (30 servings)", BigDecimal.valueOf(580000), 35)
                    )
            ),

            // 6. Mutant Mass XXXtreme 2500
            new SeedProduct(
                    "Sữa Tăng Cân Nhanh Mutant Mass XXXtreme 2500 (12 Lbs)",
                    "mutant-mass-xxxtreme-2500-12lbs",
                    "Dành riêng cho người gầy khó tăng cân lâu năm, Mutant Mass XXXtreme cung cấp tới 2540 calo và 92g protein chất lượng cao cùng nguồn tinh bột hấp thu sạch từ yến mạch và khoai lang.",
                    BigDecimal.valueOf(2150000),
                    "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80",
                    "mass-gainer",
                    List.of(
                            new SeedVariant("MUTANT-MASS-TRIPLE-CHOCO-12LBS", "Vị Triple Chocolate (12 Lbs / 5.45kg)", BigDecimal.valueOf(2150000), 30),
                            new SeedVariant("MUTANT-MASS-VANILLA-12LBS", "Vị Vanilla Ice Cream (12 Lbs / 5.45kg)", BigDecimal.valueOf(2150000), 25)
                    )
            ),

            // 7. Creatine OstroVit Monohydrate 500g
            new SeedProduct(
                    "Creatine Tăng Sức Mạnh Cơ Bắp OstroVit Monohydrate 500g",
                    "creatine-ostrovit-monohydrate-500g",
                    "Creatine Monohydrate vi hạt siêu mịn hỗ trợ tái tạo ATP nhanh chóng, gia tăng sức mạnh bộc phát, nâng cao hiệu suất tạ và kích thích tế bào cơ bắp ngậm nước nở to.",
                    BigDecimal.valueOf(420000),
                    "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=600&q=80",
                    "creatine",
                    List.of(
                            new SeedVariant("OSTROVIT-CREATINE-UNFLAVORED-500G", "Nguyên Chất Không Vị (Unflavored) 500g", BigDecimal.valueOf(420000), 90),
                            new SeedVariant("OSTROVIT-CREATINE-WATERMELON-500G", "Vị Dưa Hấu Thanh Mát 500g", BigDecimal.valueOf(450000), 40)
                    )
            ),

            // 8. Đai Lưng Tập Gym Cứng Khoá Đòn TitanFit Pro Lever Belt
            new SeedProduct(
                    "Đai Lưng Cứng Khoá Đòn TitanFit Lever Gym Belt 10mm",
                    "dai-lung-khoa-don-titanfit-lever-belt-10mm",
                    "Đai lưng da bò thật cao cấp độ dày 10mm với khoá đòn kim loại siêu bền, hỗ trợ bảo vệ tối đa cột sống và tăng áp lực ổ bụng trong các bài Squat, Deadlift nặng.",
                    BigDecimal.valueOf(850000),
                    "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&w=600&q=80",
                    "phu-kien-gym",
                    List.of(
                            new SeedVariant("BELT-LEVER-BLACK-S", "Size S (Eo 60-75cm) - Đen", BigDecimal.valueOf(850000), 20),
                            new SeedVariant("BELT-LEVER-BLACK-M", "Size M (Eo 75-90cm) - Đen", BigDecimal.valueOf(850000), 30),
                            new SeedVariant("BELT-LEVER-BLACK-L", "Size L (Eo 90-105cm) - Đen", BigDecimal.valueOf(850000), 25)
                    )
            ),

            // 9. Áo Thun Tập Gym Dry-Fit TitanFit Athletics
            new SeedProduct(
                    "Áo Thun Thể Thao Tập Gym TitanFit Dry-Fit Athletics",
                    "ao-thun-the-thao-titanfit-dry-fit-athletics",
                    "Chất liệu vải thể thao co giãn 4 chiều, công nghệ Dry-Fit thoát mồ hôi siêu tốc, giữ cơ thể luôn khô ráo và thoáng mát suốt buổi tập cường độ cao.",
                    BigDecimal.valueOf(290000),
                    "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=600&q=80",
                    "quan-ao-the-thao",
                    List.of(
                            new SeedVariant("TSHIRT-DRYFIT-BLACK-M", "Màu Đen - Size M", BigDecimal.valueOf(290000), 50),
                            new SeedVariant("TSHIRT-DRYFIT-BLACK-L", "Màu Đen - Size L", BigDecimal.valueOf(290000), 45),
                            new SeedVariant("TSHIRT-DRYFIT-GREY-M", "Màu Xám - Size M", BigDecimal.valueOf(290000), 40),
                            new SeedVariant("TSHIRT-DRYFIT-GREY-L", "Màu Xám - Size L", BigDecimal.valueOf(290000), 35)
                    )
            ),

            // 10. Bình Lắc Shaker TitanFit Inox 304 Cao Cấp 750ml
            new SeedProduct(
                    "Bình Lắc Shaker Tập Gym TitanFit Inox 304 Cao Cấp 750ml",
                    "binh-lac-shaker-titanfit-inox-304-750ml",
                    "Bình lắc kim loại inox 304 tiêu chuẩn thực phẩm, không bám mùi protein, nắp chống tràn tuyệt đối kèm quai xách tiện lợi và vạch đo dung tích chuẩn xác.",
                    BigDecimal.valueOf(220000),
                    "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=600&q=80",
                    "binh-lac-shaker",
                    List.of(
                            new SeedVariant("SHAKER-INOX-BLACK-750ML", "Màu Đen Mờ (Matte Black) 750ml", BigDecimal.valueOf(220000), 100),
                            new SeedVariant("SHAKER-INOX-SILVER-750ML", "Màu Bạc Kim Loại (Silver Metallic) 750ml", BigDecimal.valueOf(220000), 80)
                    )
            )
    );

    @Override
    @Transactional
    public void run(String... args) {
        seedProducts();
    }

    private void seedProducts() {
        for (SeedProduct seed : DEFAULT_PRODUCTS) {
            Product product = productRepository.findBySlug(seed.slug()).orElse(null);

            if (product == null) {
                Categories category = categoryRepository.findBySlug(seed.categorySlug()).orElse(null);
                if (category == null) {
                    log.warn("Category with slug '{}' not found for product '{}', skipping seeding",
                            seed.categorySlug(), seed.name());
                    continue;
                }

                product = new Product();
                product.setName(seed.name());
                product.setSlug(seed.slug());
                product.setDescription(seed.description());
                product.setBasePrice(seed.basePrice());
                product.setThumbnailUrl(seed.thumbnailUrl());
                product.setActive(true);
                product.setCategories(category);

                product = productRepository.save(product);
                log.info("Initialized default product: {} ({})", seed.name(), seed.slug());
            } else {
                log.debug("Product already exists with slug: {}", seed.slug());
            }

            // Seed variants for this product
            for (SeedVariant variantSeed : seed.variants()) {
                if (!productVariantRepository.existsBySku(variantSeed.sku())) {
                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(product);
                    variant.setSku(variantSeed.sku());
                    variant.setVariantName(variantSeed.variantName());
                    variant.setPrice(variantSeed.price());
                    variant.setStockQuantity(variantSeed.stockQuantity());

                    productVariantRepository.save(variant);
                    log.info("Initialized variant: {} ({}) for product: {}",
                            variantSeed.variantName(), variantSeed.sku(), product.getName());
                } else {
                    log.debug("Variant already exists with SKU: {}", variantSeed.sku());
                }
            }
        }
    }
}
