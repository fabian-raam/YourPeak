-- Agrega el rechazo de solicitudes y su historial a una base existente.
USE yourpeak_db;

INSERT INTO tipo_estado (id_tipo_estado, nombre, descripcion)
SELECT 4, 'RECHAZADO', 'Solicitud de acceso rechazada; el cliente puede volver a solicitar'
WHERE NOT EXISTS (SELECT 1 FROM tipo_estado WHERE nombre = 'RECHAZADO');

CREATE TABLE IF NOT EXISTS solicitud_acceso (
  id_solicitud_acceso  BIGINT        NOT NULL AUTO_INCREMENT,
  id_cliente           BIGINT        NOT NULL,
  estado               VARCHAR(15)   NOT NULL DEFAULT 'PENDIENTE',
  mensaje_cliente      VARCHAR(300)  NULL,
  motivo_rechazo       VARCHAR(500)  NULL,
  fecha_solicitud      DATETIME      NOT NULL,
  fecha_resolucion     DATETIME      NULL,
  id_administrador     BIGINT        NULL,
  PRIMARY KEY (id_solicitud_acceso),
  KEY idx_solicitud_cliente_fecha (id_cliente, fecha_solicitud),
  CONSTRAINT fk_solicitud_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
  CONSTRAINT fk_solicitud_admin   FOREIGN KEY (id_administrador) REFERENCES usuario (id_usuario),
  CONSTRAINT ck_solicitud_estado  CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA'))
) ENGINE=InnoDB;

-- Las cuentas que ya existían reciben su primera solicitud (fecha_alta está en UTC; Lima es UTC-5).
INSERT INTO solicitud_acceso (id_cliente, estado, fecha_solicitud, fecha_resolucion)
SELECT c.id_cliente,
       CASE e.nombre WHEN 'PENDIENTE' THEN 'PENDIENTE' ELSE 'APROBADA' END,
       DATE_SUB(c.fecha_alta, INTERVAL 5 HOUR),
       CASE e.nombre WHEN 'PENDIENTE' THEN NULL ELSE COALESCE(c.fecha_validacion, DATE_SUB(c.fecha_alta, INTERVAL 5 HOUR)) END
FROM cliente c
JOIN tipo_estado e ON e.id_tipo_estado = c.id_tipo_estado
WHERE NOT EXISTS (SELECT 1 FROM administrador a WHERE a.id_usuario = c.id_usuario)
  AND NOT EXISTS (SELECT 1 FROM solicitud_acceso s WHERE s.id_cliente = c.id_cliente);
