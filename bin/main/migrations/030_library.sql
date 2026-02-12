CREATE TABLE IF NOT EXISTS library_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    size BIGINT NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    INDEX idx_library_building (building_id),
    INDEX idx_library_category (category)
);
