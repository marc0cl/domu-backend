-- Consolidated inline schema updates previously in DependencyInjectionModule.runPendingMigrations()

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(100) NULL;

ALTER TABLE market_item MODIFY COLUMN main_image_url TEXT;
ALTER TABLE market_item_image MODIFY COLUMN url TEXT;
ALTER TABLE market_item_image MODIFY COLUMN box_file_id TEXT;
ALTER TABLE amenities MODIFY COLUMN image_url TEXT;
ALTER TABLE users MODIFY COLUMN avatar_box_id TEXT;
ALTER TABLE users MODIFY COLUMN privacy_avatar_box_id TEXT;

ALTER TABLE chat_participant ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMP NULL DEFAULT NULL;

ALTER TABLE buildings ADD COLUMN IF NOT EXISTS building_type VARCHAR(20) NULL;
ALTER TABLE buildings ADD COLUMN IF NOT EXISTS house_units_count INT NULL;
ALTER TABLE buildings ADD COLUMN IF NOT EXISTS apartment_units_count INT NULL;

ALTER TABLE building_requests ADD COLUMN IF NOT EXISTS building_type VARCHAR(20) NULL;
ALTER TABLE building_requests ADD COLUMN IF NOT EXISTS house_units_count INT NULL;
ALTER TABLE building_requests ADD COLUMN IF NOT EXISTS apartment_units_count INT NULL;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSON NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notif_user_building ON notifications (user_id, building_id);
CREATE INDEX IF NOT EXISTS idx_notif_user_read ON notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notif_created ON notifications (created_at DESC);

CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    in_app_enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_type (user_id, notification_type)
);
