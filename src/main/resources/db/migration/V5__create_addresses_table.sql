-- =============================================================================
-- V5: Bổ sung bảng addresses (Sổ địa chỉ người dùng) và các trường hồ sơ cá nhân
-- =============================================================================

ALTER TABLE users ADD COLUMN gender VARCHAR(50) DEFAULT NULL;
ALTER TABLE users ADD COLUMN date_of_birth VARCHAR(50) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS addresses (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    province VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    ward VARCHAR(100) DEFAULT NULL,
    detailed_address VARCHAR(255) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    label VARCHAR(50) NOT NULL DEFAULT 'Nhà Riêng',
    created_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_addresses_user_id ON addresses(user_id);
