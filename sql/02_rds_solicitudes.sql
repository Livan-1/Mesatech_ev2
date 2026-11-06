-- Ejecutar DESDE EC2 B, conectado como admin a rds-solicitudes:
--   mysql -h <endpoint-rds-solicitudes> -u admin -p < 02_rds_solicitudes.sql
-- Cambia la contraseña antes de ejecutar y NO subas la versión con la contraseña real.

CREATE DATABASE IF NOT EXISTS solicitudes_db;

CREATE USER IF NOT EXISTS 'app_solicitudes'@'%' IDENTIFIED BY 'cambiar_password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON solicitudes_db.* TO 'app_solicitudes'@'%';
FLUSH PRIVILEGES;

USE solicitudes_db;

CREATE TABLE IF NOT EXISTS solicitudes (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  titulo              VARCHAR(255) NOT NULL,
  descripcion         TEXT,
  categoria           VARCHAR(255),
  prioridad           VARCHAR(255),
  usuario_solicitante VARCHAR(255),
  estado              VARCHAR(255),
  fecha_creacion      DATETIME(6)
);

-- Copia local de las categorías recibidas por RabbitMQ.
-- categoria_id guarda el id ORIGINAL de rds-catalogo; UNIQUE evita duplicados si un mensaje se entrega dos veces.
CREATE TABLE IF NOT EXISTS categoria_local (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  categoria_id    BIGINT       NOT NULL,
  nombre          VARCHAR(100) NOT NULL,
  event_id        VARCHAR(36)  NOT NULL,
  fecha_evento    DATETIME(6)  NOT NULL,
  fecha_recepcion DATETIME(6)  NOT NULL,
  CONSTRAINT uk_categoria_local_origen UNIQUE (categoria_id)
);

SHOW GRANTS FOR 'app_solicitudes'@'%';
SHOW TABLES;
