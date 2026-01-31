-- Visit contacts (frequent visitors)
CREATE TABLE IF NOT EXISTS visit_contacts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_user_id BIGINT NOT NULL,
    visitor_name VARCHAR(150) NOT NULL,
    visitor_document VARCHAR(20),
    unit_id BIGINT,
    alias VARCHAR(80),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_visit_contacts_user FOREIGN KEY (resident_user_id) REFERENCES users(id),
    CONSTRAINT fk_visit_contacts_unit FOREIGN KEY (unit_id) REFERENCES housing_units(id)
);

CREATE INDEX idx_visit_contacts_owner ON visit_contacts (resident_user_id);
CREATE INDEX idx_visit_contacts_document ON visit_contacts (visitor_document);

