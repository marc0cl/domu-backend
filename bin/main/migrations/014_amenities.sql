-- Amenities (áreas comunes) para reservas en comunidades
-- Cada área común pertenece a un edificio/comunidad

CREATE TABLE IF NOT EXISTS amenities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    max_capacity INT,
    cost_per_slot DECIMAL(10, 2) DEFAULT 0.00,
    rules TEXT,
    image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_amenities_building FOREIGN KEY (building_id) REFERENCES buildings(id),
    CONSTRAINT chk_amenities_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE')),
    KEY idx_amenities_building (building_id),
    KEY idx_amenities_status (status)
);

-- Bloques horarios configurables por el administrador para cada área común
-- day_of_week: 1=Lunes, 2=Martes, ..., 7=Domingo
CREATE TABLE IF NOT EXISTS amenity_time_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amenity_id BIGINT NOT NULL,
    day_of_week INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_time_slots_amenity FOREIGN KEY (amenity_id) REFERENCES amenities(id) ON DELETE CASCADE,
    CONSTRAINT chk_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_time_order CHECK (start_time < end_time),
    KEY idx_time_slots_amenity (amenity_id),
    KEY idx_time_slots_day (day_of_week)
);

-- Reservas realizadas por los usuarios
CREATE TABLE IF NOT EXISTS amenity_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amenity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    time_slot_id BIGINT NOT NULL,
    reservation_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP NULL,
    CONSTRAINT fk_reservations_amenity FOREIGN KEY (amenity_id) REFERENCES amenities(id),
    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_reservations_slot FOREIGN KEY (time_slot_id) REFERENCES amenity_time_slots(id),
    CONSTRAINT chk_reservation_status CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    -- Evitar reservas duplicadas para el mismo slot y fecha
    CONSTRAINT uq_reservation_slot_date UNIQUE (time_slot_id, reservation_date, status),
    KEY idx_reservations_amenity (amenity_id),
    KEY idx_reservations_user (user_id),
    KEY idx_reservations_date (reservation_date),
    KEY idx_reservations_status (status)
);
