-- Agregar campos de auditoría a housing_units para rastrear creación y modificaciones
-- También prepara la tabla para soft delete

-- Paso 1: Agregar columna created_by_user_id
ALTER TABLE housing_units 
  ADD COLUMN created_by_user_id BIGINT NULL COMMENT 'Usuario que creó la unidad';

-- Paso 2: Agregar constraint de FK para created_by_user_id
ALTER TABLE housing_units
  ADD CONSTRAINT fk_housing_units_creator 
    FOREIGN KEY (created_by_user_id) REFERENCES users(id);

-- Paso 3: Agregar columna created_at (si no existe)
-- Primero verificamos si existe, si no, la agregamos
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'housing_units' 
               AND COLUMN_NAME = 'created_at');

SET @sqlstmt := IF(@exist = 0, 
  'ALTER TABLE housing_units ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT ''Fecha de creación de la unidad''',
  'SELECT ''Column created_at already exists'' AS message');

PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Paso 4: Agregar columna updated_at
ALTER TABLE housing_units 
  ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización';

-- Paso 5: Actualizar valores de status existentes para asegurar consistencia
-- Actualizar cualquier valor NULL o inválido a 'ACTIVE'
UPDATE housing_units 
SET status = 'ACTIVE' 
WHERE status IS NULL OR status = '';

-- Nota: La columna status ya existe con VARCHAR(20) DEFAULT 'ACTIVE'
-- Los valores permitidos son: 'ACTIVE', 'INACTIVE', 'DELETED'
