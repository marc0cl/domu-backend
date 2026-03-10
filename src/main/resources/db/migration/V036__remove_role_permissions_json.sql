-- Align the authorization schema with the current RBAC implementation
ALTER TABLE roles
    DROP COLUMN permissions_json;
