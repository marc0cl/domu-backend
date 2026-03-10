-- Sincronizar user_buildings: agregar usuarios con unit_id que no estén en user_buildings
-- para el edificio de su unidad (multi-tenant).
INSERT IGNORE INTO user_buildings (user_id, building_id)
SELECT u.id, hu.building_id
FROM users u
JOIN housing_units hu ON hu.id = u.unit_id
LEFT JOIN user_buildings ub ON ub.user_id = u.id AND ub.building_id = hu.building_id
WHERE ub.user_id IS NULL AND u.unit_id IS NOT NULL;
