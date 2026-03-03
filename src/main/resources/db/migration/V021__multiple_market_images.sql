-- Migration for Multiple Marketplace Images
-- Author: Gemini CLI
-- Date: 2026-02-01

CREATE TABLE IF NOT EXISTS market_item_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    box_file_id VARCHAR(100) NOT NULL,
    url TEXT NOT NULL,
    is_main BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_market_image_item FOREIGN KEY (item_id) REFERENCES market_item(id) ON DELETE CASCADE
);

-- Index for faster retrieval
CREATE INDEX idx_market_image_item ON market_item_image(item_id);
