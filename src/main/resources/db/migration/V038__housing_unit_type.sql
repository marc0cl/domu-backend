ALTER TABLE housing_units
    ADD COLUMN unit_type VARCHAR(20) DEFAULT 'DEPARTAMENTO' AFTER number;

UPDATE housing_units SET unit_type = 'DEPARTAMENTO' WHERE unit_type IS NULL;
