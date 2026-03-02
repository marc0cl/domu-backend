-- Flujo de alta de comunidad: solicitud y aprobación de edificio

-- Nuevos atributos en buildings
ALTER TABLE buildings ADD COLUMN tower_label VARCHAR(150) NULL;
ALTER TABLE buildings ADD COLUMN owner_user_id BIGINT NULL;
ALTER TABLE buildings
    ADD CONSTRAINT fk_buildings_owner FOREIGN KEY (owner_user_id) REFERENCES users(id);
ALTER TABLE buildings ADD COLUMN latitude DECIMAL(10, 7) NULL;
ALTER TABLE buildings ADD COLUMN longitude DECIMAL(10, 7) NULL;
ALTER TABLE buildings ADD COLUMN building_type VARCHAR(20) NULL;
ALTER TABLE buildings ADD COLUMN house_units_count INT NULL;
ALTER TABLE buildings ADD COLUMN apartment_units_count INT NULL;

-- Solicitudes de creación de edificio
CREATE TABLE IF NOT EXISTS building_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requested_by_user_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    tower_label VARCHAR(150),
    address VARCHAR(255) NOT NULL,
    commune VARCHAR(100),
    city VARCHAR(100),
    admin_phone VARCHAR(50),
    admin_email VARCHAR(150),
    admin_name VARCHAR(150),
    admin_document VARCHAR(100),
    building_type VARCHAR(20),
    floors INT,
    units_count INT,
    house_units_count INT,
    apartment_units_count INT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    proof_text MEDIUMTEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_by_user_id BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    review_notes VARCHAR(255),
    building_id BIGINT NULL,
    CONSTRAINT fk_br_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_br_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_br_building FOREIGN KEY (building_id) REFERENCES buildings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

