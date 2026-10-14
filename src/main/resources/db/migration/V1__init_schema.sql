-- =============================================================================
-- TITANFIT SPORTS & SUPPLEMENTS — DATABASE MIGRATION SCRIPT (MYSQL 8)
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. BẢNG USERS (Tài khoản người dùng: Khách mua hàng & Quản trị viên)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) DEFAULT NULL,
    email VARCHAR(255) DEFAULT NULL,
    full_name VARCHAR(255) DEFAULT NULL,
    password VARCHAR(255) DEFAULT NULL,
    phone VARCHAR(255) DEFAULT NULL,
    role VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- 2. BẢNG CATEGORIES (Danh mục phân loại sản phẩm)
CREATE TABLE IF NOT EXISTS categories (
    id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) DEFAULT NULL,
    category_tree VARCHAR(255) DEFAULT NULL,
    image_url VARCHAR(255) DEFAULT NULL,
    name VARCHAR(255) DEFAULT NULL,
    slug VARCHAR(255) DEFAULT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_categories_slug ON categories(slug);

-- 3. BẢNG ORDERS (Đơn đặt hàng từ khách)
CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) DEFAULT NULL,
    customer_name VARCHAR(255) DEFAULT NULL,
    note VARCHAR(255) DEFAULT NULL,
    order_code VARCHAR(255) DEFAULT NULL,
    order_status VARCHAR(255) DEFAULT NULL,
    payment_method VARCHAR(255) DEFAULT NULL,
    payment_status VARCHAR(255) DEFAULT NULL,
    phone VARCHAR(255) DEFAULT NULL,
    shipping_address VARCHAR(255) DEFAULT NULL,
    total_amount DECIMAL(38,2) DEFAULT NULL,
    user_id VARCHAR(36) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_code (order_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_orders_order_code ON orders(order_code);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_order_status ON orders(order_status);

-- 4. BẢNG PRODUCT_VARIANTS (Biến thể SKU đặc thù)
CREATE TABLE IF NOT EXISTS product_variants (
    id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) DEFAULT NULL,
    price DECIMAL(38,2) DEFAULT NULL,
    sku VARCHAR(255) DEFAULT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    variant_name VARCHAR(255) DEFAULT NULL,
    product_id VARCHAR(36) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_variants_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_variants_product_id ON product_variants(product_id);
CREATE INDEX idx_variants_sku ON product_variants(sku);

-- 5. BẢNG ORDER_ITEM (Chi tiết mặt hàng trong đơn)
CREATE TABLE IF NOT EXISTS order_item (
    id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) DEFAULT NULL,
    price DECIMAL(38,2) DEFAULT NULL,
    product_name VARCHAR(255) DEFAULT NULL,
    quantity INT NOT NULL DEFAULT 1,
    variant_name VARCHAR(255) DEFAULT NULL,
    order_id VARCHAR(36) DEFAULT NULL,
    variant_id VARCHAR(36) DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_order_item_order_id ON order_item(order_id);
CREATE INDEX idx_order_item_variant_id ON order_item(variant_id);

-- 6. BẢNG PRODUCTS (Thông tin sản phẩm chính)
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) DEFAULT NULL,
    base_price DECIMAL(38,2) DEFAULT NULL,
    description LONGTEXT DEFAULT NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    name VARCHAR(255) DEFAULT NULL,
    slug VARCHAR(255) DEFAULT NULL,
    thumbnail_url VARCHAR(255) DEFAULT NULL,
    category_id VARCHAR(36) DEFAULT NULL,
    order_item_id VARCHAR(36) DEFAULT NULL,
    product_id VARCHAR(36) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_products_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_slug ON products(slug);

-- 7. BẢNG REVIEWS (Đánh giá & nhận xét sản phẩm)
CREATE TABLE IF NOT EXISTS reviews (
    id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) DEFAULT NULL,
    comment LONGTEXT DEFAULT NULL,
    rating INT NOT NULL DEFAULT 5,
    product_id VARCHAR(36) DEFAULT NULL,
    user_id VARCHAR(36) DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);

-- THIẾT LẬP RÀNG BUỘC KHÓA NGOẠI (FOREIGN KEYS)
ALTER TABLE orders 
    ADD CONSTRAINT FK_orders_user FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE product_variants 
    ADD CONSTRAINT FK_product_variants_product FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE order_item 
    ADD CONSTRAINT FK_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
    ADD CONSTRAINT FK_order_item_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id);

ALTER TABLE products 
    ADD CONSTRAINT FK_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
    ADD CONSTRAINT FK_products_order_item FOREIGN KEY (order_item_id) REFERENCES order_item(id),
    ADD CONSTRAINT FK_products_order_item_prod FOREIGN KEY (product_id) REFERENCES order_item(id);

ALTER TABLE reviews 
    ADD CONSTRAINT FK_reviews_product FOREIGN KEY (product_id) REFERENCES products(id),
    ADD CONSTRAINT FK_reviews_user FOREIGN KEY (user_id) REFERENCES users(id);

-- SEED DATA CHO DANH MỤC
INSERT INTO categories (id, name, slug, category_tree, image_url, sort_order, created_at)
VALUES
    ('c0000001-0000-0000-0000-000000000001', 'Đồ Thể Thao & Phụ Kiện Gym', 'do-the-thao', 'do-the-thao', 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=400&q=80', 1, NOW()),
    ('c0000002-0000-0000-0000-000000000002', 'Dinh Dưỡng & Thực Phẩm Bổ Sung', 'thuc-pham-bo-sung', 'thuc-pham-bo-sung', 'https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?auto=format&fit=crop&w=400&q=80', 2, NOW())
AS new_data
ON DUPLICATE KEY UPDATE name = new_data.name;

SET FOREIGN_KEY_CHECKS = 1;