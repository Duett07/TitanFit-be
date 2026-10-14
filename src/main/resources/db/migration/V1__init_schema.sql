-- =============================================================================
-- TITANFIT SPORTS & SUPPLEMENTS — DATABASE MIGRATION SCRIPT
-- =============================================================================

-- 1. BẢNG USERS (Tài khoản người dùng: Khách mua hàng & Quản trị viên Shop)
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Mật khẩu băm BCrypt
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_CUSTOMER' CHECK (role IN ('ROLE_CUSTOMER', 'ROLE_ADMIN')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- 2. BẢNG CATEGORIES (Danh mục phân loại: Đồ thể thao vs Thực phẩm bổ sung)
CREATE TABLE IF NOT EXISTS categories (
                                          id BIGSERIAL PRIMARY KEY,
                                          name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    category_type VARCHAR(30) NOT NULL CHECK (category_type IN ('SPORTS_GEAR', 'SUPPLEMENTS')),
    image_url VARCHAR(500),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);
CREATE INDEX IF NOT EXISTS idx_categories_type ON categories(category_type);

-- 3. BẢNG PRODUCTS (Thông tin sản phẩm chính)
CREATE TABLE IF NOT EXISTS products (
                                        id BIGSERIAL PRIMARY KEY,
                                        category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    base_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    thumbnail_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                                                                                                  );

CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_slug ON products(slug);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON products(is_active);

-- 4. BẢNG PRODUCT_VARIANTS (Biến thể SKU đặc thù: Size/Màu hoặc Vị/Khối lượng)
CREATE TABLE IF NOT EXISTS product_variants (
                                                id BIGSERIAL PRIMARY KEY,
                                                product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(100) NOT NULL UNIQUE,
    variant_name VARCHAR(150) NOT NULL, -- Ví dụ: "Size L, Màu Đen" hoặc "Vị Chocolate, 5lbs"
    price NUMERIC(15, 2) NOT NULL,       -- Giá bán riêng của từng biến thể
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                                                                                                       );

CREATE INDEX IF NOT EXISTS idx_variants_product_id ON product_variants(product_id);
CREATE INDEX IF NOT EXISTS idx_variants_sku ON product_variants(sku);

-- 5. BẢNG ORDERS (Đơn đặt hàng từ khách)
CREATE TABLE IF NOT EXISTS orders (
                                      id BIGSERIAL PRIMARY KEY,
                                      user_id BIGINT REFERENCES users(id) ON DELETE SET NULL, -- Nullable cho khách vãng lai
    order_code VARCHAR(50) NOT NULL UNIQUE,                 -- Mã đơn hàng: TF-883921
    customer_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    shipping_address TEXT NOT NULL,
    note TEXT,
    payment_method VARCHAR(20) NOT NULL DEFAULT 'COD' CHECK (payment_method IN ('COD', 'VIETQR')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID', 'PAID')),
    order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (order_status IN ('PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED')),
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                                                                              );

CREATE INDEX IF NOT EXISTS idx_orders_order_code ON orders(order_code);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_status ON orders(order_status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);

-- 6. BẢNG ORDER_ITEMS (Chi tiết mặt hàng - Lưu snapshot tên và giá)
CREATE TABLE IF NOT EXISTS order_items (
                                           id BIGSERIAL PRIMARY KEY,
                                           order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id) ON DELETE SET NULL,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL,
    product_name VARCHAR(255) NOT NULL,
    variant_name VARCHAR(150) NOT NULL,
    price NUMERIC(15, 2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                                                                                              );

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);

-- 7. BẢNG REVIEWS (Đánh giá sao & nhận xét sản phẩm)
CREATE TABLE IF NOT EXISTS reviews (
                                       id BIGSERIAL PRIMARY KEY,
                                       product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                                                                                              );

CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON reviews(product_id);

-- SEED SẴN TÀI KHOẢN MẪU & DANH MỤC:
INSERT INTO users (email, password, full_name, phone, role)
VALUES
    ('admin@titanfit.vn', '$2a$10$w3eQvV82eM3jYqVqYx3mUe1M5xP9h8v3Y2zM1o4P6r8t0u2w4y6A', 'Quản Trị Viên TITANFIT', '0901234567', 'ROLE_ADMIN'),
    ('hoang.nguyen@example.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'Nguyễn Văn Hoàng', '0912345678', 'ROLE_CUSTOMER')
    ON CONFLICT (email) DO NOTHING;

INSERT INTO categories (name, slug, category_type, image_url, sort_order)
VALUES
    ('Đồ Thể Thao & Phụ Kiện Gym', 'do-the-thao', 'SPORTS_GEAR', 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=400&q=80', 1),
    ('Dinh Dưỡng & Thực Phẩm Bổ Sung', 'thuc-pham-bo-sung', 'SUPPLEMENTS', 'https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?auto=format&fit=crop&w=400&q=80', 2)
    ON CONFLICT (slug) DO NOTHING;