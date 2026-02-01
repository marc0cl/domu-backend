-- User confirmation flow enhancements

-- Create user_confirmations table
CREATE TABLE IF NOT EXISTS user_confirmations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    confirmed_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_confirmation_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Index for token lookup
CREATE INDEX idx_user_confirmations_token ON user_confirmations (token);

-- Update users table to allow PENDING status
-- (No change needed to schema as status is already VARCHAR(20))
