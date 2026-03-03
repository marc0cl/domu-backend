-- Crear tabla staff relacionada con building_id
-- Los buildings ya funcionan como comunidades, por lo que staff se relaciona directamente con building_id
CREATE TABLE IF NOT EXISTS staff (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    first_name VARCHAR(120) NOT NULL,
    last_name VARCHAR(120) NOT NULL,
    rut VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255),
    phone VARCHAR(50),
    position VARCHAR(120) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_building FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE
);

-- Agregar índices para búsquedas
-- Nota: Si la tabla ya existe sin índices, ejecuta solo estas dos líneas:
-- CREATE INDEX idx_staff_building_id ON staff(building_id);
-- CREATE INDEX idx_staff_active ON staff(active);
