-- Token para invitar a crear usuario administrador tras la aprobación
ALTER TABLE building_requests
    ADD COLUMN admin_invite_code VARCHAR(100) NULL,
    ADD COLUMN admin_invite_expires_at TIMESTAMP NULL,
    ADD COLUMN admin_invite_used_at TIMESTAMP NULL;

CREATE UNIQUE INDEX idx_building_requests_admin_invite_code ON building_requests(admin_invite_code);

