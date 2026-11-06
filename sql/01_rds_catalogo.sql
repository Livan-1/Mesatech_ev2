-- Ejecutar DESDE EC2 A, conectado como admin a rds-catalogo:
--   mysql -h <endpoint-rds-catalogo> -u admin -p < 01_rds_catalogo.sql
-- Cambia la contraseña antes de ejecutar y NO subas la versión con la contraseña real.

CREATE DATABASE IF NOT EXISTS catalogo_db;

CREATE USER IF NOT EXISTS 'app_catalogo'@'%' IDENTIFIED BY 'cambiar_password';
-- Permisos solo sobre su propia base (ms-catalogo crea su tabla con ddl-auto=update)
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON catalogo_db.* TO 'app_catalogo'@'%';
FLUSH PRIVILEGES;

USE catalogo_db;

CREATE TABLE IF NOT EXISTS catalogo (
  id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(255) NOT NULL,
  tipo   VARCHAR(255) NOT NULL   -- CATEGORIA o PRIORIDAD
);

SHOW GRANTS FOR 'app_catalogo'@'%';
SHOW TABLES;
