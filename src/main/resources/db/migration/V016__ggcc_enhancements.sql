-- Mejoras de gastos comunes (GGCC): historial, boletas y metadata

-- Periodos: auditoría básica y responsable
ALTER TABLE common_expense_periods ADD COLUMN created_by_user_id BIGINT NULL;
ALTER TABLE common_expense_periods ADD COLUMN updated_by_user_id BIGINT NULL;
ALTER TABLE common_expense_periods ADD COLUMN updated_at TIMESTAMP NULL;

-- Cargos: origen y boletas (Box)
ALTER TABLE common_charges ADD COLUMN origin VARCHAR(120) NULL;
ALTER TABLE common_charges ADD COLUMN receipt_file_id VARCHAR(120) NULL;
ALTER TABLE common_charges ADD COLUMN receipt_file_name VARCHAR(255) NULL;
ALTER TABLE common_charges ADD COLUMN receipt_folder_id VARCHAR(120) NULL;
ALTER TABLE common_charges ADD COLUMN receipt_mime_type VARCHAR(120) NULL;
ALTER TABLE common_charges ADD COLUMN receipt_uploaded_at TIMESTAMP NULL;

-- Historial de correcciones/ajustes
CREATE TABLE IF NOT EXISTS common_expense_revisions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_id BIGINT NOT NULL,
    created_by_user_id BIGINT NULL,
    action VARCHAR(30) NOT NULL,
    note TEXT NULL,
    changes_json MEDIUMTEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_revision_period FOREIGN KEY (period_id) REFERENCES common_expense_periods (id),
    CONSTRAINT fk_revision_user FOREIGN KEY (created_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
