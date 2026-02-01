-- Migration for Community Marketplace and Real-time Chat
-- Corrected to match "users" and "buildings" naming convention
-- Author: Gemini CLI
-- Date: 2026-02-01

-- Marketplace Categories
CREATE TABLE IF NOT EXISTS market_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(50),
    display_order INT DEFAULT 0
);

-- Marketplace Items
CREATE TABLE IF NOT EXISTS market_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    building_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(15, 2) NOT NULL,
    original_price_link VARCHAR(500),
    status VARCHAR(20) DEFAULT 'AVAILABLE', -- AVAILABLE, SOLD, ARCHIVED
    box_folder_id VARCHAR(100), -- Folder in Box for this item's images
    main_image_url VARCHAR(500), -- Preview image
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_market_item_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_market_item_category FOREIGN KEY (category_id) REFERENCES market_category(id)
);

-- Chat Rooms
CREATE TABLE IF NOT EXISTS chat_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    item_id BIGINT NULL, -- Optional: Link to a marketplace item
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_room_item FOREIGN KEY (item_id) REFERENCES market_item(id)
);

-- Chat Participants
CREATE TABLE IF NOT EXISTS chat_participant (
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_typing BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (room_id, user_id),
    CONSTRAINT fk_chat_participant_room FOREIGN KEY (room_id) REFERENCES chat_room(id),
    CONSTRAINT fk_chat_participant_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Chat Messages
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT,
    type VARCHAR(20) DEFAULT 'TEXT', -- TEXT, IMAGE, AUDIO
    box_file_id VARCHAR(100), -- Box reference for audio/images
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_message_room FOREIGN KEY (room_id) REFERENCES chat_room(id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES users(id)
);

-- Seed basic categories
INSERT INTO market_category (name, icon, display_order) VALUES 
('Hogar y Muebles', 'home', 1),
('Tecnología', 'cpuChip', 2),
('Servicios', 'wrench', 3),
('Alimentos', 'shoppingBag', 4),
('Otros', 'archiveBox', 5);