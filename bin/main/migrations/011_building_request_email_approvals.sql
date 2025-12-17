-- Tokens y auditoría para aprobación/rechazo vía correo
ALTER TABLE building_requests
    ADD COLUMN approval_code VARCHAR(100) NULL,
    ADD COLUMN approval_code_expires_at TIMESTAMP NULL,
    ADD COLUMN approval_code_used_at TIMESTAMP NULL,
    ADD COLUMN approval_action VARCHAR(20) NULL;

CREATE UNIQUE INDEX idx_building_requests_approval_code ON building_requests(approval_code);

-- Razones más largas
ALTER TABLE building_requests
    MODIFY review_notes TEXT NULL;

