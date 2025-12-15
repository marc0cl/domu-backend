-- Relación usuario-edificio para soportar múltiples edificios por usuario
CREATE TABLE IF NOT EXISTS user_buildings (
    user_id BIGINT NOT NULL,
    building_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, building_id),
    CONSTRAINT fk_user_building_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_building_building FOREIGN KEY (building_id) REFERENCES buildings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear edificio por defecto si no existe
INSERT INTO buildings (name, address, commune, city, status)
SELECT 'Departamentos Topo', 'Sin dirección', NULL, NULL, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM buildings WHERE name = 'Departamentos Topo'
);

-- Vincular usuarios con su edificio según la unidad
INSERT INTO user_buildings (user_id, building_id)
SELECT u.id, hu.building_id
FROM users u
JOIN housing_units hu ON hu.id = u.unit_id
LEFT JOIN user_buildings ub ON ub.user_id = u.id AND ub.building_id = hu.building_id
WHERE ub.user_id IS NULL;

-- Asignar edificio por defecto a usuarios sin unidad
INSERT INTO user_buildings (user_id, building_id)
SELECT u.id, b.id
FROM users u
JOIN buildings b ON b.name = 'Departamentos Topo'
LEFT JOIN user_buildings ub ON ub.user_id = u.id
WHERE u.unit_id IS NULL AND ub.user_id IS NULL;

