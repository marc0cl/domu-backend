-- Gestión de gastos comunes y pagos parciales

-- Nota: MySQL < 8.0.29 no soporta ADD COLUMN IF NOT EXISTS en lista; se ejecuta en dos sentencias.
ALTER TABLE buildings ADD COLUMN floors INT NULL;
ALTER TABLE buildings ADD COLUMN units_count INT NULL;

CREATE TABLE IF NOT EXISTS common_expense_periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    generated_at DATE NOT NULL,
    due_date DATE NOT NULL,
    reserve_amount DECIMAL(12, 2) DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN',
    UNIQUE KEY uix_building_period (building_id, year, month),
    CONSTRAINT fk_period_building FOREIGN KEY (building_id) REFERENCES buildings (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS common_charges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_id BIGINT NOT NULL,
    unit_id BIGINT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    prorateable BOOLEAN DEFAULT FALSE,
    payer_type VARCHAR(30) DEFAULT 'RESIDENT',
    receipt_text MEDIUMTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_charge_period FOREIGN KEY (period_id) REFERENCES common_expense_periods (id),
    CONSTRAINT fk_charge_unit FOREIGN KEY (unit_id) REFERENCES housing_units (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS common_payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    charge_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    issued_at DATE NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    reference VARCHAR(255),
    receipt_text MEDIUMTEXT,
    status VARCHAR(20) DEFAULT 'CONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_charge FOREIGN KEY (charge_id) REFERENCES common_charges (id),
    CONSTRAINT fk_payment_unit FOREIGN KEY (unit_id) REFERENCES housing_units (id),
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS delinquency_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    period_id BIGINT NOT NULL,
    balance DECIMAL(12, 2) NOT NULL,
    days_delinquent INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uix_unit_period (unit_id, period_id),
    CONSTRAINT fk_delinquency_unit FOREIGN KEY (unit_id) REFERENCES housing_units (id),
    CONSTRAINT fk_delinquency_period FOREIGN KEY (period_id) REFERENCES common_expense_periods (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

