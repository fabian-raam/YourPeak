-- Actualiza una base existente conservando sus usuarios y contraseñas.
USE yourpeak_db;

ALTER TABLE usuario
  ADD COLUMN google_subject VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NULL,
  ADD UNIQUE KEY uq_usuario_google_subject (google_subject);
