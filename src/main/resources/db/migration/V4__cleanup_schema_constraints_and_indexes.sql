-- =============================================================================
-- V4: Chuẩn hóa schema, loại bỏ index trùng lặp, bổ sung NOT NULL và CHECK constraints
-- =============================================================================

-- 1. Dọn dẹp danh mục mẫu từ migration V1 nếu chưa có sản phẩm liên kết
DELETE c FROM categories c
LEFT JOIN products p ON c.id = p.category_id
WHERE c.slug IN ('do-the-thao', 'thuc-pham-bo-sung')
  AND p.id IS NULL;

-- 2. Loại bỏ các index trùng lặp với UNIQUE KEY
ALTER TABLE users DROP INDEX idx_users_email;
ALTER TABLE categories DROP INDEX idx_categories_slug;
ALTER TABLE orders DROP INDEX idx_orders_order_code;
ALTER TABLE product_variants DROP INDEX idx_variants_sku;
ALTER TABLE products DROP INDEX idx_products_slug;

-- 3. Chuẩn hóa dữ liệu NULL trước khi áp dụng ràng buộc NOT NULL
UPDATE users SET full_name = 'Người dùng' WHERE full_name IS NULL;
UPDATE users SET role = 'ROLE_CUSTOMER' WHERE role IS NULL;

UPDATE categories SET name = slug WHERE name IS NULL;

UPDATE products SET name = 'Sản phẩm chưa đặt tên' WHERE name IS NULL;
UPDATE products SET base_price = 0.00 WHERE base_price IS NULL OR base_price < 0;

UPDATE product_variants SET variant_name = 'Mặc định' WHERE variant_name IS NULL;
UPDATE product_variants SET stock_quantity = 0 WHERE stock_quantity IS NULL OR stock_quantity < 0;

UPDATE orders SET customer_name = 'Khách hàng' WHERE customer_name IS NULL;
UPDATE orders SET phone = '0000000000' WHERE phone IS NULL;
UPDATE orders SET shipping_address = 'Chưa cung cấp' WHERE shipping_address IS NULL;
UPDATE orders SET total_amount = 0.00 WHERE total_amount IS NULL OR total_amount < 0;
UPDATE orders SET order_status = 'PENDING' WHERE order_status IS NULL;
UPDATE orders SET payment_status = 'UNPAID' WHERE payment_status IS NULL;
UPDATE orders SET payment_method = 'COD' WHERE payment_method IS NULL;

UPDATE order_item SET product_name = 'Sản phẩm' WHERE product_name IS NULL;
UPDATE order_item SET quantity = 1 WHERE quantity IS NULL OR quantity <= 0;

-- 4. Bổ sung ràng buộc NOT NULL cho các cột bắt buộc
ALTER TABLE users 
    MODIFY COLUMN email VARCHAR(255) NOT NULL,
    MODIFY COLUMN password VARCHAR(255) NOT NULL,
    MODIFY COLUMN full_name VARCHAR(255) NOT NULL,
    MODIFY COLUMN role VARCHAR(50) NOT NULL;

ALTER TABLE categories 
    MODIFY COLUMN name VARCHAR(255) NOT NULL,
    MODIFY COLUMN slug VARCHAR(255) NOT NULL;

ALTER TABLE products 
    MODIFY COLUMN name VARCHAR(255) NOT NULL,
    MODIFY COLUMN slug VARCHAR(255) NOT NULL,
    MODIFY COLUMN base_price DECIMAL(38,2) NOT NULL DEFAULT 0.00;

ALTER TABLE product_variants 
    MODIFY COLUMN sku VARCHAR(255) NOT NULL,
    MODIFY COLUMN variant_name VARCHAR(255) NOT NULL,
    MODIFY COLUMN stock_quantity INT NOT NULL DEFAULT 0,
    MODIFY COLUMN product_id VARCHAR(36) NOT NULL;

ALTER TABLE orders 
    MODIFY COLUMN order_code VARCHAR(255) NOT NULL,
    MODIFY COLUMN customer_name VARCHAR(255) NOT NULL,
    MODIFY COLUMN phone VARCHAR(50) NOT NULL,
    MODIFY COLUMN shipping_address VARCHAR(255) NOT NULL,
    MODIFY COLUMN total_amount DECIMAL(38,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN order_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    MODIFY COLUMN payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    MODIFY COLUMN payment_method VARCHAR(50) NOT NULL;

ALTER TABLE order_item 
    MODIFY COLUMN order_id VARCHAR(36) NOT NULL,
    MODIFY COLUMN product_name VARCHAR(255) NOT NULL,
    MODIFY COLUMN quantity INT NOT NULL DEFAULT 1;

-- 5. Bổ sung các ràng buộc CHECK bảo vệ dữ liệu nghiệp vụ
ALTER TABLE product_variants 
    ADD CONSTRAINT chk_product_variants_price CHECK (price >= 0),
    ADD CONSTRAINT chk_product_variants_stock CHECK (stock_quantity >= 0);

ALTER TABLE products 
    ADD CONSTRAINT chk_products_base_price CHECK (base_price >= 0);

ALTER TABLE orders 
    ADD CONSTRAINT chk_orders_total_amount CHECK (total_amount >= 0);

ALTER TABLE order_item 
    ADD CONSTRAINT chk_order_item_price CHECK (price >= 0),
    ADD CONSTRAINT chk_order_item_quantity CHECK (quantity > 0);
