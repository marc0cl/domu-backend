-- Schema for authentication and basic user data
CREATE TABLE IF NOT EXISTS rol (
    id_rol BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS unidad (
    id_unidad BIGINT AUTO_INCREMENT PRIMARY KEY,
    identificador VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_unidad BIGINT NULL,
    id_rol BIGINT NULL,
    nombres VARCHAR(150) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    fecha_nacimiento DATE NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(50),
    documento VARCHAR(100),
    es_residente BOOLEAN,
    password_hash VARCHAR(255) NOT NULL,
    estado VARCHAR(20) DEFAULT 'ACTIVE',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_unidad FOREIGN KEY (id_unidad) REFERENCES unidad(id_unidad),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
);

-- Seed basic roles to simplify initial logins
INSERT INTO rol (nombre) VALUES ('admin'), ('residente')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);
