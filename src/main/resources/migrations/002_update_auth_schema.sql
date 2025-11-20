-- Refresh auth-related schema to align with the current domain models

-- Remove legacy tables if they exist
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS rol;
DROP TABLE IF EXISTS unidad;

-- Roles catalog
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    permissions_json TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- Buildings catalog (supports housing units)
CREATE TABLE IF NOT EXISTS buildings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    commune VARCHAR(100),
    city VARCHAR(100),
    admin_phone VARCHAR(50),
    admin_email VARCHAR(150),
    created_at DATE DEFAULT CURRENT_DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- Housing units catalog
CREATE TABLE IF NOT EXISTS housing_units (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    number VARCHAR(50) NOT NULL,
    tower VARCHAR(50),
    floor VARCHAR(50),
    aliquot_percentage DECIMAL(8, 4),
    square_meters DECIMAL(10, 2),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    CONSTRAINT fk_housing_units_building FOREIGN KEY (building_id) REFERENCES buildings(id)
);

-- Core user table used for authentication
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NULL,
    role_id BIGINT NULL,
    first_name VARCHAR(150) NOT NULL,
    last_name VARCHAR(150) NOT NULL,
    birth_date DATE NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    resident BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_unit FOREIGN KEY (unit_id) REFERENCES housing_units(id),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Seed initial roles to simplify local setup
INSERT INTO roles (name, description)
VALUES ('admin', 'Administrador del sistema'),
       ('resident', 'Residente del condominio')
ON DUPLICATE KEY UPDATE description = VALUES(description);
