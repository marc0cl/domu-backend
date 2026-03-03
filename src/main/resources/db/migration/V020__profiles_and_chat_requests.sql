-- Migration for Neighbor Profiles and Chat Request System
-- Author: Gemini CLI
-- Date: 2026-02-01

-- Enhance user profiles
ALTER TABLE users ADD COLUMN bio TEXT NULL;
ALTER TABLE users ADD COLUMN avatar_box_id VARCHAR(100) NULL;

-- Chat Request System
CREATE TABLE IF NOT EXISTS chat_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    building_id BIGINT NOT NULL,
    item_id BIGINT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    initial_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_req_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_chat_req_receiver FOREIGN KEY (receiver_id) REFERENCES users(id),
    CONSTRAINT fk_chat_req_item FOREIGN KEY (item_id) REFERENCES market_item(id)
);

-- Index for performance
CREATE INDEX idx_chat_request_receiver ON chat_request(receiver_id, status);
CREATE INDEX idx_chat_request_building ON chat_request(building_id);
