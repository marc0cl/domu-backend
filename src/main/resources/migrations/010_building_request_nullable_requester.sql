-- Permitir solicitudes de comunidad sin usuario autenticado
ALTER TABLE building_requests
    MODIFY requested_by_user_id BIGINT NULL;

