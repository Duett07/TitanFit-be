-- V3: Xoá bỏ các ràng buộc khoá ngoại và cột thừa giữa Product và OrderItem
-- 1. Bỏ ràng buộc khoá ngoại từ bảng products trỏ sang order_item
ALTER TABLE products DROP FOREIGN KEY FK_products_order_item;
ALTER TABLE products DROP FOREIGN KEY FK_products_order_item_prod;

-- 2. Bỏ các cột order_item_id và product_id thừa trong bảng products
ALTER TABLE products DROP COLUMN order_item_id;
ALTER TABLE products DROP COLUMN product_id;
