-- Agregar rol 'proveedor' si no existe
INSERT IGNORE INTO roles (name, description) VALUES ('proveedor', 'Proveedor externo de servicios');

-- Tabla de proveedores
CREATE TABLE IF NOT EXISTS providers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    business_name VARCHAR(200) NOT NULL,
    rut VARCHAR(20) NOT NULL,
    contact_name VARCHAR(150),
    email VARCHAR(255),
    phone VARCHAR(50),
    address VARCHAR(255),
    service_category VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_providers_building FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE,
    CONSTRAINT fk_providers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_providers_building ON providers(building_id);
CREATE INDEX idx_providers_active ON providers(active);

-- Tabla de ordenes de servicio
CREATE TABLE IF NOT EXISTS service_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    scheduled_date DATE,
    status VARCHAR(30) DEFAULT 'PENDING',
    priority VARCHAR(20) DEFAULT 'NORMAL',
    admin_notes TEXT,
    provider_notes TEXT,
    completed_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_so_building FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE,
    CONSTRAINT fk_so_provider FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE CASCADE,
    CONSTRAINT fk_so_creator FOREIGN KEY (created_by) REFERENCES users(id)
);
CREATE INDEX idx_so_building ON service_orders(building_id);
CREATE INDEX idx_so_provider ON service_orders(provider_id);
CREATE INDEX idx_so_status ON service_orders(status);

-- Tabla de cotizaciones
CREATE TABLE IF NOT EXISTS quotations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_order_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    description TEXT,
    valid_until DATE,
    file_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_quot_order FOREIGN KEY (service_order_id) REFERENCES service_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_quot_provider FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE CASCADE
);
