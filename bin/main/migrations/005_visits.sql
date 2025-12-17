-- Visit authorization and access tracking
CREATE TABLE IF NOT EXISTS visits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visitor_name VARCHAR(150) NOT NULL,
    visitor_document VARCHAR(20),
    visitor_type VARCHAR(30) DEFAULT 'VISIT',
    company VARCHAR(150),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS visit_authorizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_id BIGINT NOT NULL,
    resident_user_id BIGINT NOT NULL,
    unit_id BIGINT NOT NULL,
    valid_from DATETIME NOT NULL,
    valid_until DATETIME NOT NULL,
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    qr_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_visit_auth_visit FOREIGN KEY (visit_id) REFERENCES visits(id),
    CONSTRAINT fk_visit_auth_user FOREIGN KEY (resident_user_id) REFERENCES users(id),
    CONSTRAINT fk_visit_auth_unit FOREIGN KEY (unit_id) REFERENCES housing_units(id)
);

CREATE TABLE IF NOT EXISTS visit_access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_id BIGINT NOT NULL,
    authorization_id BIGINT,
    recorded_at DATETIME NOT NULL,
    door VARCHAR(100),
    authorized_by_user_id BIGINT,
    outcome VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_visit_logs_visit FOREIGN KEY (visit_id) REFERENCES visits(id),
    CONSTRAINT fk_visit_logs_auth FOREIGN KEY (authorization_id) REFERENCES visit_authorizations(id),
    CONSTRAINT fk_visit_logs_user FOREIGN KEY (authorized_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_visit_auth_resident ON visit_authorizations (resident_user_id);
CREATE INDEX idx_visit_auth_valid_until ON visit_authorizations (valid_until);

