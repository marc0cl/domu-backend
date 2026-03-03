-- Agregar índices a la tabla staff (ejecutar solo si la tabla ya existe sin índices)
-- Este script verifica si los índices existen antes de crearlos

-- Verificar y crear índice idx_staff_building_id
SET @dbname = DATABASE();
SET @tablename = 'staff';
SET @indexname = 'idx_staff_building_id';

SELECT COUNT(*) INTO @index_exists
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = @dbname
AND TABLE_NAME = @tablename
AND INDEX_NAME = @indexname;

SET @sql = IF(@index_exists = 0,
    CONCAT('CREATE INDEX ', @indexname, ' ON ', @tablename, '(building_id)'),
    'SELECT ''Index already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verificar y crear índice idx_staff_active
SET @indexname = 'idx_staff_active';

SELECT COUNT(*) INTO @index_exists
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = @dbname
AND TABLE_NAME = @tablename
AND INDEX_NAME = @indexname;

SET @sql = IF(@index_exists = 0,
    CONCAT('CREATE INDEX ', @indexname, ' ON ', @tablename, '(active)'),
    'SELECT ''Index already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
