CREATE TABLE IF NOT EXISTS parcels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    unit_id BIGINT NOT NULL,
    received_by_user_id BIGINT NOT NULL,
    sender VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    received_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    retrieved_at DATETIME NULL,
    retrieved_by_user_id BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_parcels_building FOREIGN KEY (building_id) REFERENCES buildings(id),
    CONSTRAINT fk_parcels_unit FOREIGN KEY (unit_id) REFERENCES housing_units(id),
    CONSTRAINT fk_parcels_received_by FOREIGN KEY (received_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_parcels_retrieved_by FOREIGN KEY (retrieved_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_parcels_building_status ON parcels (building_id, status);
CREATE INDEX idx_parcels_unit_status ON parcels (unit_id, status);
CREATE INDEX idx_parcels_received_at ON parcels (received_at);
