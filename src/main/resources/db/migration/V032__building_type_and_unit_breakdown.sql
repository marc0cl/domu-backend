-- Add community type and unit breakdown fields for houses/apartments/mixed.

SET @exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'buildings'
    AND COLUMN_NAME = 'building_type'
);
SET @sqlstmt := IF(@exists = 0,
  'ALTER TABLE buildings ADD COLUMN building_type VARCHAR(20) NULL',
  'SELECT ''building_type already exists in buildings''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'buildings'
    AND COLUMN_NAME = 'house_units_count'
);
SET @sqlstmt := IF(@exists = 0,
  'ALTER TABLE buildings ADD COLUMN house_units_count INT NULL',
  'SELECT ''house_units_count already exists in buildings''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'buildings'
    AND COLUMN_NAME = 'apartment_units_count'
);
SET @sqlstmt := IF(@exists = 0,
  'ALTER TABLE buildings ADD COLUMN apartment_units_count INT NULL',
  'SELECT ''apartment_units_count already exists in buildings''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'building_requests'
    AND COLUMN_NAME = 'building_type'
);
SET @sqlstmt := IF(@exists = 0,
  'ALTER TABLE building_requests ADD COLUMN building_type VARCHAR(20) NULL',
  'SELECT ''building_type already exists in building_requests''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'building_requests'
    AND COLUMN_NAME = 'house_units_count'
);
SET @sqlstmt := IF(@exists = 0,
  'ALTER TABLE building_requests ADD COLUMN house_units_count INT NULL',
  'SELECT ''house_units_count already exists in building_requests''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'building_requests'
    AND COLUMN_NAME = 'apartment_units_count'
);
SET @sqlstmt := IF(@exists = 0,
  'ALTER TABLE building_requests ADD COLUMN apartment_units_count INT NULL',
  'SELECT ''apartment_units_count already exists in building_requests''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
