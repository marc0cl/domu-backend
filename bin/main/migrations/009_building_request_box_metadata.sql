-- Metadatos de Box para solicitudes de comunidad
ALTER TABLE building_requests
    ADD COLUMN box_folder_id VARCHAR(100) NULL,
    ADD COLUMN box_file_id VARCHAR(100) NULL,
    ADD COLUMN box_file_name VARCHAR(255) NULL;
