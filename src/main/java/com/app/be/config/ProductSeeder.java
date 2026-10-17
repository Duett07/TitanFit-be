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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@Order(3)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
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
            // ==========================================
            // I. 10 ĐỒ THỂ THAO & PHỤ KIỆN TẬP LUYỆN
            // ==========================================

            // 1. Áo Thun Nam Pro-Combat Ôm Sát Giữ Cơ
            new SeedProduct(
                    "Áo Thun Thể Thao Pro-Combat Giữ Cơ Co Giãn 4 Chiều",
                    "ao-thun-tap-gym-pro-combat",
                    "Áo thun Pro-Combat ôm sát body (compression) công nghệ Dry-Fit siêu thoáng khí, co giãn 4 chiều hỗ trợ giữ ấm và định hình nhóm cơ ngực vai, giảm ma sát tối đa khi vận động cường độ cao.",
                    BigDecimal.valueOf(260000),
                    "https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=800&h=800&q=80",
                    "quan-ao-the-thao",
                    List.of(
                            new SeedVariant("PRO-COMBAT-BLK-M", "Màu Đen Titan - Size M (55-65kg)", BigDecimal.valueOf(260000), 50),
                            new SeedVariant("PRO-COMBAT-BLK-L", "Màu Đen Titan - Size L (65-75kg)", BigDecimal.valueOf(260000), 45),
                            new SeedVariant("PRO-COMBAT-BLK-XL", "Màu Đen Titan - Size XL (75-88kg)", BigDecimal.valueOf(260000), 30),
                            new SeedVariant("PRO-COMBAT-GRY-M", "Màu Xám Khói - Size M (55-65kg)", BigDecimal.valueOf(260000), 40)
                    )
            ),

            // 2. Quần Tập Gym Nam 2 Lớp Co Giãn Có Túi Lót Tiện Lợi
            new SeedProduct(
                    "Quần Tập Gym Nam 2 Lớp TitanFit Kèm Túi Lót Điện Thoại",
                    "quan-tap-gym-2-lop-nam-titanfit",
                    "Thiết kế 2 lớp thông minh gồm lớp lót co giãn 4 chiều chống cọ xát cơ đùi và lớp ngoài gió thể thao siêu nhẹ. Tích hợp ngăn túi ẩn ôm sát đựng điện thoại chống rơi và quai gài khăn tập sau lưng tiện lợi.",
                    BigDecimal.valueOf(280000),
                    "https://images.unsplash.com/photo-1591195853828-11db59a44f6b?auto=format&fit=crop&w=800&h=800&q=80",
                    "quan-ao-the-thao",
                    List.of(
                            new SeedVariant("SHORT-2IN1-BLK-M", "Màu Đen Đậm - Size M (55-68kg)", BigDecimal.valueOf(280000), 60),
                            new SeedVariant("SHORT-2IN1-BLK-L", "Màu Đen Đậm - Size L (68-80kg)", BigDecimal.valueOf(280000), 50),
                            new SeedVariant("SHORT-2IN1-GRN-L", "Màu Xanh Rêu Quân Đội - Size L (68-80kg)", BigDecimal.valueOf(280000), 35)
                    )
            ),

            // 3. Áo Thun Thể Thao Tập Gym TitanFit Dry-Fit Athletics
            new SeedProduct(
                    "Áo Thun Thể Thao Tập Gym TitanFit Dry-Fit Athletics",
                    "ao-thun-the-thao-titanfit-dry-fit-athletics",
                    "Chất liệu vải thể thao co giãn 4 chiều, công nghệ Dry-Fit thoát mồ hôi siêu tốc, giữ cơ thể luôn khô ráo và thoáng mát suốt buổi tập cường độ cao.",
                    BigDecimal.valueOf(290000),
                    "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=800&h=800&q=80",
                    "quan-ao-the-thao",
                    List.of(
                            new SeedVariant("TSHIRT-DRYFIT-BLACK-M", "Màu Đen - Size M", BigDecimal.valueOf(290000), 50),
                            new SeedVariant("TSHIRT-DRYFIT-BLACK-L", "Màu Đen - Size L", BigDecimal.valueOf(290000), 45),
                            new SeedVariant("TSHIRT-DRYFIT-GREY-M", "Màu Xám - Size M", BigDecimal.valueOf(290000), 40),
                            new SeedVariant("TSHIRT-DRYFIT-GREY-L", "Màu Xám - Size L", BigDecimal.valueOf(290000), 35)
                    )
            ),

            // 4. Giày Chạy Bộ & Tập Gym Đa Năng TitanFit Running Pro
            new SeedProduct(
                    "Giày Chạy Bộ Thể Thao Thoáng Khí TitanFit Running Pro",
                    "giay-chay-bo-titanfit-running-pro",
                    "Giày thể thao đa năng đế đệm EVA cao cấp hấp thụ lực chấn động tối đa, thân giày dệt sợi Flyknit thoáng khí siêu bền, đế ngoài cao su ma sát cao bám dính chắc chắn trên máy chạy lẫn đường trường.",
                    BigDecimal.valueOf(890000),
                    "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&h=800&q=80",
                    "quan-ao-the-thao",
                    List.of(
                            new SeedVariant("SHOE-RUNPRO-RED-40", "Màu Đỏ Thể Thao - Size 40", BigDecimal.valueOf(890000), 20),
                            new SeedVariant("SHOE-RUNPRO-RED-41", "Màu Đỏ Thể Thao - Size 41", BigDecimal.valueOf(890000), 30),
                            new SeedVariant("SHOE-RUNPRO-RED-42", "Màu Đỏ Thể Thao - Size 42", BigDecimal.valueOf(890000), 35),
                            new SeedVariant("SHOE-RUNPRO-RED-43", "Màu Đỏ Thể Thao - Size 43", BigDecimal.valueOf(890000), 25)
                    )
            ),

            // 5. Bình Lắc Shaker TitanFit Inox 304 Cao Cấp 700ml
            new SeedProduct(
                    "Bình Lắc Shaker Tập Gym TitanFit Inox 304 Cao Cấp 700ml",
                    "binh-lac-shaker-titanfit-inox-304-750ml",
                    "Bình lắc kim loại thép không gỉ Inox 304 tiêu chuẩn thực phẩm dung tích 700ml, không bám mùi protein, nắp chống tràn tuyệt đối kèm quai xách tiện lợi và vạch đo dung tích dập nổi chuẩn xác.",
                    BigDecimal.valueOf(220000),
                    "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=800&h=800&q=80",
                    "binh-lac-shaker",
                    List.of(
                            new SeedVariant("SHAKER-INOX-BLACK-750ML", "Màu Đen Mờ (Matte Black) 700ml", BigDecimal.valueOf(220000), 100),
                            new SeedVariant("SHAKER-INOX-SILVER-750ML", "Màu Bạc Kim Loại (Silver Metallic) 700ml", BigDecimal.valueOf(220000), 80)
                    )
            ),

            // 6. Đai Lưng Cứng Khoá Đòn TitanFit Lever Gym Belt 10mm
            new SeedProduct(
                    "Đai Lưng Cứng Khoá Đòn TitanFit Lever Gym Belt 10mm",
                    "dai-lung-khoa-don-titanfit-lever-belt-10mm",
                    "Đai lưng da bò thật cao cấp độ dày 10mm với khoá đòn hợp kim đúc siêu bền, hỗ trợ bảo vệ tối đa cột sống và tăng áp lực ổ bụng trong các bài tập nặng như Squat, Deadlift.",
                    BigDecimal.valueOf(850000),
                    "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&w=800&h=800&q=80",
                    "phu-kien-gym",
                    List.of(
                            new SeedVariant("BELT-LEVER-BLACK-S", "Size S (Eo 60-75cm) - Đen", BigDecimal.valueOf(850000), 20),
                            new SeedVariant("BELT-LEVER-BLACK-M", "Size M (Eo 75-90cm) - Đen", BigDecimal.valueOf(850000), 30),
                            new SeedVariant("BELT-LEVER-BLACK-L", "Size L (Eo 90-105cm) - Đen", BigDecimal.valueOf(850000), 25)
                    )
            ),

            // 7. Băng Quấn Cổ Tay Trợ Lực Đẩy Tạ TitanFit Heavy Wrist Wraps
            new SeedProduct(
                    "Băng Quấn Cổ Tay Trợ Lực Đẩy Tạ TitanFit Heavy Wrist Wraps",
                    "bang-quan-co-tay-titanfit-heavy-wrist-wraps",
                    "Quấn cổ tay chuyên dụng cho Powerlifting và tập gym nặng, chiều dài 50cm từ sợi dệt đàn hồi gia cường chịu tải lớn. Băng dán Velcro chắc chắn giúp khóa cứng cổ tay, giảm đau nhức và phòng tránh chấn thương khớp khi đẩy tạ nặng.",
                    BigDecimal.valueOf(190000),
                    "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&h=800&q=80",
                    "phu-kien-gym",
                    List.of(
                            new SeedVariant("WRIST-WRAP-REDLINE-PAIR", "Bản Đen Viền Đỏ (Đôi 50cm)", BigDecimal.valueOf(190000), 80),
                            new SeedVariant("WRIST-WRAP-STEALTH-PAIR", "Bản Đen Toàn Bộ Stealth Black (Đôi 50cm)", BigDecimal.valueOf(190000), 60)
                    )
            ),

            // 8. Găng Tay Tập Gym Có Đệm Da Chống Chai Tay TitanFit Grip Pro
            new SeedProduct(
                    "Găng Tay Tập Gym Thoáng Khí Chống Chai Tay TitanFit Grip Pro",
                    "gang-tay-tap-gym-titanfit-grip-pro",
                    "Thiết kế hở ngón thông thoáng cùng lớp đệm da lộn chống trượt và hạt silicon tổ ong tăng độ bám tạ tối ưu. Mu bàn tay sợi dệt co giãn tản nhiệt nhanh kết hợp đai quấn cổ tay bảo vệ đa năng.",
                    BigDecimal.valueOf(210000),
                    "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&w=800&h=800&q=80",
                    "phu-kien-gym",
                    List.of(
                            new SeedVariant("GLOVE-GRIP-M", "Size M (Chu vi lòng bàn tay 18-20cm)", BigDecimal.valueOf(210000), 50),
                            new SeedVariant("GLOVE-GRIP-L", "Size L (Chu vi lòng bàn tay 20-22cm)", BigDecimal.valueOf(210000), 45),
                            new SeedVariant("GLOVE-GRIP-XL", "Size XL (Chu vi lòng bàn tay 22-24cm)", BigDecimal.valueOf(210000), 30)
                    )
            ),

            // 9. Bộ Dây Kháng Lực Ngũ Sắc Tập Toàn Thân TitanFit 150LBS
            new SeedProduct(
                    "Bộ Dây Kháng Lực Ngũ Sắc Tập Thể Lực TitanFit Resistance Bands 150LBS",
                    "bo-day-khang-luc-ngu-sac-titanfit-150lbs",
                    "Bộ 5 dây ống cao su latex tự nhiên siêu bền với 5 mức kháng lực khác nhau từ 10 lbs đến 50 lbs (tổng kháng lực 150 lbs). Đi kèm 2 tay cầm xốp mềm, 2 đai cổ chân, 1 chốt neo cửa và túi đựng thể thao nhỏ gọn.",
                    BigDecimal.valueOf(350000),
                    "https://images.unsplash.com/photo-1598289431512-b97b0917affc?auto=format&fit=crop&w=800&h=800&q=80",
                    "phu-kien-gym",
                    List.of(
                            new SeedVariant("BAND-SET-150LBS", "Bộ 11 Món Tiêu Chuẩn 150 LBS", BigDecimal.valueOf(350000), 70),
                            new SeedVariant("BAND-SET-PRO-200LBS", "Bộ 11 Món Nâng Cao Heavy Duty 200 LBS", BigDecimal.valueOf(420000), 40)
                    )
            ),

            // 10. Con Lăn Tập Bụng Bánh Kép Tự Động Thu Hồi TitanFit Ab Roller
            new SeedProduct(
                    "Con Lăn Tập Cơ Bụng Bánh Xe Đôi Tự Động Thu Hồi TitanFit Ab Roller",
                    "con-lan-tap-bung-banh-kep-titanfit-ab-roller",
                    "Con lăn tập bụng bánh kép bản rộng vững chãi, tích hợp lò xo trợ lực thông minh tự động phanh và bật về giúp người tập gập bụng dễ dàng, không đau mỏi lưng dưới. Tặng kèm thảm lót đầu gối dày êm ái.",
                    BigDecimal.valueOf(320000),
                    "https://images.unsplash.com/photo-1517963879433-6ad2b056d712?auto=format&fit=crop&w=800&h=800&q=80",
                    "phu-kien-gym",
                    List.of(
                            new SeedVariant("AB-ROLLER-SPRING-RED", "Màu Đỏ Đen Có Lò Xo Trợ Lực", BigDecimal.valueOf(320000), 45),
                            new SeedVariant("AB-ROLLER-SPRING-BLUE", "Màu Xanh Đen Có Lò Xo Trợ Lực", BigDecimal.valueOf(320000), 35)
                    )
            ),

            // ==========================================
            // II. 10 THỰC PHẨM BỔ SUNG & DINH DƯỠNG THỂ HÌNH
            // ==========================================

            // 11. Sữa Tăng Cơ Optimum Nutrition Whey Gold Standard 100%
            new SeedProduct(
                    "Sữa Tăng Cơ Optimum Nutrition Whey Gold Standard 100% (5 Lbs)",
                    "whey-gold-standard-100",
                    "Whey Gold Standard là dòng sản phẩm whey protein bán chạy số 1 thế giới với sự kết hợp tối ưu giữa Whey Isolate, Whey Concentrate và Hydrolyzed Whey Peptides giúp phục hồi và phát triển cơ bắp vượt trội.",
                    BigDecimal.valueOf(1550000),
                    "https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?auto=format&fit=crop&w=800&h=800&q=80",
                    "whey-protein",
                    List.of(
                            new SeedVariant("WHEY-GOLD-CHOCO-5LBS", "Vị Double Rich Chocolate (5 Lbs / 2.27kg)", BigDecimal.valueOf(1550000), 50),
                            new SeedVariant("WHEY-GOLD-VANILLA-5LBS", "Vị Vanilla Ice Cream (5 Lbs / 2.27kg)", BigDecimal.valueOf(1550000), 35),
                            new SeedVariant("WHEY-GOLD-STRAWBERRY-5LBS", "Vị Delicious Strawberry (5 Lbs / 2.27kg)", BigDecimal.valueOf(1590000), 20)
                    )
            ),

            // 12. Sữa Tăng Cơ Mutant Iso Surge 100% Whey Isolate 5lbs
            new SeedProduct(
                    "Sữa Tăng Cơ Tinh Khiết Mutant Iso Surge 100% Whey Isolate (5 Lbs)",
                    "mutant-iso-surge-5lbs",
                    "Mutant Iso Surge cung cấp 25g Protein siêu tinh khiết từ Whey Isolate & Hydrolyzed hấp thu nhanh, vị ngon đỉnh cao đoạt nhiều giải thưởng quốc tế, không đường thừa, hỗ trợ siết cơ nét căng.",
                    BigDecimal.valueOf(1850000),
                    "https://images.unsplash.com/photo-1593095948071-474c5cc2989d?auto=format&fit=crop&w=800&h=800&q=80",
                    "whey-protein",
                    List.of(
                            new SeedVariant("MUTANT-ISO-TRIPLE-CHOCO-5LBS", "Vị Triple Chocolate (5 Lbs / 2.27kg)", BigDecimal.valueOf(1850000), 40),
                            new SeedVariant("MUTANT-ISO-PEANUT-5LBS", "Vị Chocolate Peanut Butter (5 Lbs / 2.27kg)", BigDecimal.valueOf(1850000), 30),
                            new SeedVariant("MUTANT-ISO-COOKIES-5LBS", "Vị Cookies & Cream (5 Lbs / 2.27kg)", BigDecimal.valueOf(1890000), 25)
                    )
            ),

            // 13. Sữa Tăng Cơ Rule 1 Proteins R1 Protein Isolate
            new SeedProduct(
                    "Sữa Tăng Cơ Rule 1 Proteins R1 Protein Isolate (5 Lbs)",
                    "rule-1-protein-isolate",
                    "R1 Protein với thành phần 100% Whey Protein Isolate & Hydrolysate siêu tinh khiết, hầu như không chứa đường, fat, gluten hay chất phụ gia, hỗ trợ hấp thu cực nhanh sau tập luyện.",
                    BigDecimal.valueOf(1650000),
                    "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=800&h=800&q=80",
                    "whey-protein",
                    List.of(
                            new SeedVariant("R1-ISOLATE-CHOCO-FUDGE-5LBS", "Vị Chocolate Fudge (5 Lbs / 2.23kg)", BigDecimal.valueOf(1650000), 45),
                            new SeedVariant("R1-ISOLATE-CAFE-MOCHA-5LBS", "Vị Cafe Mocha (5 Lbs / 2.23kg)", BigDecimal.valueOf(1690000), 30)
                    )
            ),

            // 14. Creatine OstroVit Monohydrate 500g
            new SeedProduct(
                    "Creatine Tăng Sức Mạnh Cơ Bắp OstroVit Monohydrate 500g",
                    "creatine-ostrovit-monohydrate-500g",
                    "Creatine Monohydrate vi hạt siêu mịn hỗ trợ tái tạo ATP nhanh chóng, gia tăng sức mạnh bộc phát, nâng cao hiệu suất tạ và kích thích tế bào cơ bắp ngậm nước nở to.",
                    BigDecimal.valueOf(420000),
                    "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=800&h=800&q=80",
                    "creatine",
                    List.of(
                            new SeedVariant("OSTROVIT-CREATINE-UNFLAVORED-500G", "Nguyên Chất Không Vị (Unflavored) 500g", BigDecimal.valueOf(420000), 90),
                            new SeedVariant("OSTROVIT-CREATINE-WATERMELON-500G", "Vị Dưa Hấu Thanh Mát 500g", BigDecimal.valueOf(450000), 40)
                    )
            ),

            // 15. Pre-Workout Cellucor C4 Original 30 Servings
            new SeedProduct(
                    "Bột Tăng Năng Lượng Tỉnh Táo Cellucor C4 Original 30 Servings",
                    "cellucor-c4-original-pre-workout",
                    "C4 Original huyền thoại của thương hiệu Cellucor mang lại năng lượng bền bỉ, cảm giác bơm cơ ấn tượng và tăng sức bền cơ bắp nhờ CarnoSyn Beta-Alanine và Arginine AKG.",
                    BigDecimal.valueOf(680000),
                    "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&w=800&h=800&q=80",
                    "pre-workout",
                    List.of(
                            new SeedVariant("C4-ORIGINAL-ICY-BLUE-30S", "Vị Icy Blue Razz (30 lần dùng)", BigDecimal.valueOf(680000), 45),
                            new SeedVariant("C4-ORIGINAL-FRUIT-PUNCH-30S", "Vị Fruit Punch (30 lần dùng)", BigDecimal.valueOf(680000), 50)
                    )
            ),

            // 16. Pre-Workout Applied Nutrition ABE 315g
            new SeedProduct(
                    "Tăng Năng Lượng & Bơm Cơ Applied Nutrition ABE Pre-Workout 315g",
                    "applied-nutrition-abe-pre-workout",
                    "ABE Pre-Workout mang lại nguồn năng lượng bùng nổ, tăng sức mạnh và khả năng tập trung cao độ, đẩy giới hạn bài tập lên mức tối đa với TeaCrine, Citrulline Malate và Beta-Alanine.",
                    BigDecimal.valueOf(650000),
                    "https://images.unsplash.com/photo-1526401485004-46910ecc8e51?auto=format&fit=crop&w=800&h=800&q=80",
                    "pre-workout",
                    List.of(
                            new SeedVariant("ABE-PRE-ENERGY-FLAVOR-315G", "Vị Energy Flavor (30 lần dùng)", BigDecimal.valueOf(650000), 60),
                            new SeedVariant("ABE-PRE-SOUR-APPLE-315G", "Vị Táo Chua Sour Apple (30 lần dùng)", BigDecimal.valueOf(650000), 40),
                            new SeedVariant("ABE-PRE-TROPICAL-315G", "Vị Tropical Crush (30 lần dùng)", BigDecimal.valueOf(650000), 30)
                    )
            ),

            // 17. Scivation Xtend BCAA 30 Servings
            new SeedProduct(
                    "Phục Hồi Cơ Bắp & Bù Điện Giải Scivation Xtend BCAA 30 Servings",
                    "scivation-xtend-bcaa-30-servings",
                    "Xtend BCAA chứa tỷ lệ vàng 2:1:1 (Leucine, Isoleucine, Valine) kết hợp chất điện giải Electrolyte giúp chống dị hóa cơ bắp, bù nước và giảm đau mỏi cơ bắp trong và sau khi tập.",
                    BigDecimal.valueOf(580000),
                    "https://images.unsplash.com/photo-1550258987-190a2d41a8ba?auto=format&fit=crop&w=800&h=800&q=80",
                    "bcaa-amino",
                    List.of(
                            new SeedVariant("XTEND-BCAA-WATERMELON-30S", "Vị Dưa Hấu Watermelon (30 servings)", BigDecimal.valueOf(580000), 75),
                            new SeedVariant("XTEND-BCAA-MANGO-30S", "Vị Xoài Mango Madness (30 servings)", BigDecimal.valueOf(580000), 60),
                            new SeedVariant("XTEND-BCAA-GRAPE-30S", "Vị Nho Glacial Grape (30 servings)", BigDecimal.valueOf(580000), 35)
                    )
            ),

            // 18. Mutant Mass XXXtreme 2500 12 Lbs
            new SeedProduct(
                    "Sữa Tăng Cân Nhanh Cho Người Gầy Mutant Mass XXXtreme 2500 (12 Lbs)",
                    "mutant-mass-xxxtreme-2500-12lbs",
                    "Dành riêng cho người gầy khó tăng cân lâu năm, Mutant Mass XXXtreme cung cấp tới 2540 calo và 92g protein chất lượng cao cùng nguồn tinh bột hấp thu sạch từ yến mạch và khoai lang.",
                    BigDecimal.valueOf(2150000),
                    "https://images.unsplash.com/photo-1534367507873-d2d7e24c797f?auto=format&fit=crop&w=800&h=800&q=80",
                    "mass-gainer",
                    List.of(
                            new SeedVariant("MUTANT-MASS-TRIPLE-CHOCO-12LBS", "Vị Triple Chocolate (12 Lbs / 5.45kg)", BigDecimal.valueOf(2150000), 30),
                            new SeedVariant("MUTANT-MASS-VANILLA-12LBS", "Vị Vanilla Ice Cream (12 Lbs / 5.45kg)", BigDecimal.valueOf(2150000), 25)
                    )
            ),

            // 19. Dầu Cá Đậm Đặc Now Foods Ultra Omega-3 180 Viên
            new SeedProduct(
                    "Dầu Cá Đậm Đặc Tinh Khiết Now Foods Ultra Omega-3 (180 Viên)",
                    "dau-ca-now-foods-ultra-omega-3-180-vien",
                    "Dầu cá tinh khiết chưng cất phân tử đạt chuẩn GMP, cung cấp 500mg EPA và 250mg DHA mỗi viên nang mềm có màng bọc chống ợ tanh, hỗ trợ tim mạch, thị lực và bôi trơn các khớp xương khi nâng tạ nặng.",
                    BigDecimal.valueOf(620000),
                    "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?auto=format&fit=crop&w=800&h=800&q=80",
                    "vitamin-khoang-chat",
                    List.of(
                            new SeedVariant("NOW-OMEGA3-180SG", "Hộp 180 Viên Nang Mềm Softgels", BigDecimal.valueOf(620000), 80),
                            new SeedVariant("NOW-OMEGA3-90SG", "Hộp 90 Viên Nang Mềm Softgels", BigDecimal.valueOf(380000), 50)
                    )
            ),

            // 20. Vitamin Tổng Hợp & Khoáng Chất Toàn Diện Mutant Multi 60 Viên
            new SeedProduct(
                    "Vitamin Tổng Hợp Cho Người Tập Gym Mutant Multi Complete (60 Viên)",
                    "vitamin-tong-hop-mutant-multi-60-vien",
                    "Công thức hoàn chỉnh với hơn 80 vi chất dinh dưỡng, enzym tiêu hóa và chiết xuất thảo dược tăng sức bền đặc chế riêng cho gymer và vận động viên, giúp tối ưu hóa quá trình trao đổi chất và phục hồi thể lực.",
                    BigDecimal.valueOf(390000),
                    "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?auto=format&fit=crop&w=800&h=800&q=80",
                    "vitamin-khoang-chat",
                    List.of(
                            new SeedVariant("MUTANT-MULTI-60TAB", "Hộp 60 Viên Nén (Dùng 60 Ngày)", BigDecimal.valueOf(390000), 65),
                            new SeedVariant("MUTANT-MULTI-COMBO-2BOX", "Combo 2 Hộp Tiết Kiệm (120 Viên Nén)", BigDecimal.valueOf(720000), 30)
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
                // Đồng bộ ảnh tỉ lệ vuông chuẩn Unsplash nếu sản phẩm đã tồn tại
                if (product.getThumbnailUrl() != null && !product.getThumbnailUrl().contains("h=800")) {
                    product.setThumbnailUrl(seed.thumbnailUrl());
                    productRepository.save(product);
                    log.info("Synchronized square aspect ratio thumbnail for existing product: {}", product.getName());
                }
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
