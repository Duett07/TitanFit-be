-- V2: Đảm bảo toàn vẹn dữ liệu giá sản phẩm và chi tiết đơn hàng
-- Cập nhật giá các biến thể đang NULL hoặc <= 0 bằng base_price của sản phẩm cha (nếu có)
UPDATE product_variants pv
JOIN products p ON pv.product_id = p.id
SET pv.price = p.base_price
WHERE pv.price IS NULL OR pv.price <= 0;

-- Cập nhật bất kỳ biến thể nào vẫn còn NULL về 0.00 trước khi áp dụng NOT NULL
UPDATE product_variants
SET price = 0.00
WHERE price IS NULL;

-- Cập nhật các bản ghi order_item đang NULL giá về 0.00 trước khi áp dụng NOT NULL
UPDATE order_item
SET price = 0.00
WHERE price IS NULL;

-- Áp dụng ràng buộc NOT NULL cho cột price ở bảng product_variants
ALTER TABLE product_variants MODIFY COLUMN price DECIMAL(38,2) NOT NULL;

-- Áp dụng ràng buộc NOT NULL cho cột price ở bảng order_item
ALTER TABLE order_item MODIFY COLUMN price DECIMAL(38,2) NOT NULL;
