-- =====================================================================
-- YourPeak - Esquema de base de datos (MySQL 8 / MariaDB 10.5+)
-- Fase 1 = Sprint 1 (HU-09, 10, 13, 14, 15, 16)
-- Fase 2 = resto de epicas (entrenamiento IA, membresias, reportes)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS yourpeak_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE yourpeak_db;

SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 1. USUARIOS Y ROLES                                   [Fase 1]
-- =====================================================================

CREATE TABLE usuario (
  id_usuario        BIGINT        NOT NULL AUTO_INCREMENT,
  nombre            VARCHAR(100)  NOT NULL,
  apellido          VARCHAR(100)  NOT NULL,
  correo            VARCHAR(254)  NOT NULL,           -- correo institucional Tecsup (HU-15)
  contrasena_hash   VARCHAR(255)  NOT NULL,
  fecha_nacimiento  DATE          NULL,               -- NULL: HU-15 no la exige al registrarse
  fecha_creacion    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_usuario),
  UNIQUE KEY uq_usuario_correo (correo)
) ENGINE=InnoDB;

CREATE TABLE administrador (
  id_administrador  BIGINT NOT NULL AUTO_INCREMENT,
  id_usuario        BIGINT NOT NULL,
  PRIMARY KEY (id_administrador),
  UNIQUE KEY uq_admin_usuario (id_usuario),
  CONSTRAINT fk_admin_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Catalogos de perfil
-- ---------------------------------------------------------------------
CREATE TABLE tipo_estado (            -- catalogo compartido de estados
  id_tipo_estado  INT          NOT NULL AUTO_INCREMENT,
  nombre          VARCHAR(30)  NOT NULL,
  descripcion     VARCHAR(150) NULL,
  PRIMARY KEY (id_tipo_estado),
  UNIQUE KEY uq_tipo_estado_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE tipo_objetivo (
  id_tipo_objetivo  INT          NOT NULL AUTO_INCREMENT,
  nombre            VARCHAR(50)  NOT NULL,
  descripcion       VARCHAR(150) NULL,
  PRIMARY KEY (id_tipo_objetivo),
  UNIQUE KEY uq_tipo_objetivo_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE nivel_experiencia (      -- "nivel fisico" de HU-09
  id_nivel_experiencia  INT          NOT NULL AUTO_INCREMENT,
  nombre                VARCHAR(30)  NOT NULL,
  descripcion           VARCHAR(150) NULL,
  PRIMARY KEY (id_nivel_experiencia),
  UNIQUE KEY uq_nivel_exp_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE nivel_actividad (        -- factor de actividad para calcular macros
  id_nivel_actividad  INT           NOT NULL AUTO_INCREMENT,
  nombre              VARCHAR(30)   NOT NULL,
  descripcion         VARCHAR(150)  NULL,
  PRIMARY KEY (id_nivel_actividad),
  UNIQUE KEY uq_nivel_act_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE cliente (
  id_cliente                 BIGINT   NOT NULL AUTO_INCREMENT,
  id_usuario                 BIGINT   NOT NULL,
  id_tipo_estado             INT      NOT NULL DEFAULT 1,   -- 1 = PENDIENTE al registrarse
  id_tipo_objetivo           INT      NULL,                 -- se completa en el perfil (HU-09)
  id_nivel_experiencia       INT      NULL,
  id_nivel_actividad         INT      NULL,
  fecha_validacion           DATETIME NULL,
  fecha_alta                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_cliente),
  UNIQUE KEY uq_cliente_usuario (id_usuario),
  CONSTRAINT fk_cliente_usuario  FOREIGN KEY (id_usuario)                 REFERENCES usuario (id_usuario),
  CONSTRAINT fk_cliente_estado   FOREIGN KEY (id_tipo_estado)             REFERENCES tipo_estado (id_tipo_estado),
  CONSTRAINT fk_cliente_objetivo FOREIGN KEY (id_tipo_objetivo)           REFERENCES tipo_objetivo (id_tipo_objetivo),
  CONSTRAINT fk_cliente_nivelexp FOREIGN KEY (id_nivel_experiencia)       REFERENCES nivel_experiencia (id_nivel_experiencia),
  CONSTRAINT fk_cliente_nivelact FOREIGN KEY (id_nivel_actividad)         REFERENCES nivel_actividad (id_nivel_actividad)
) ENGINE=InnoDB;

CREATE TABLE registro_fisico (
  id_registro_fisico  BIGINT       NOT NULL AUTO_INCREMENT,
  id_cliente          BIGINT       NOT NULL,
  peso_kg             DECIMAL(5,2) NOT NULL,
  talla_cm            DECIMAL(5,2) NOT NULL,
  fecha_registro      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  observacion         VARCHAR(500) NULL,
  PRIMARY KEY (id_registro_fisico),
  KEY idx_regfis_cliente_fecha (id_cliente, fecha_registro),
  CONSTRAINT fk_regfis_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
  CONSTRAINT ck_regfis_valores CHECK (peso_kg > 0 AND talla_cm > 0)
) ENGINE=InnoDB;

-- =====================================================================
-- 2. DISPONIBILIDAD DE ENTRENAMIENTO (HU-10)            [Fase 1]
-- =====================================================================

CREATE TABLE disponibilidad_cliente (
  id_disponibilidad  BIGINT   NOT NULL AUTO_INCREMENT,
  id_cliente         BIGINT   NOT NULL,
  dia_semana         TINYINT  NOT NULL,        -- 1 = lunes ... 7 = domingo
  hora_inicio        TIME     NOT NULL,
  hora_fin           TIME     NOT NULL,
  PRIMARY KEY (id_disponibilidad),
  UNIQUE KEY uq_disp_cliente_dia (id_cliente, dia_semana),
  CONSTRAINT fk_disp_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
  CONSTRAINT ck_disp_dia   CHECK (dia_semana BETWEEN 1 AND 7),
  CONSTRAINT ck_disp_horas CHECK (hora_fin > hora_inicio)
) ENGINE=InnoDB;

-- =====================================================================
-- 3. NUTRICION: catalogo, metas e ingestas (HU-13)
-- =====================================================================

CREATE TABLE alimento (                                       -- [Fase 1] catalogo offline
  id_alimento      INT           NOT NULL AUTO_INCREMENT,
  nombre           VARCHAR(120)  NOT NULL,
  porcion_base_g   DECIMAL(6,2)  NOT NULL DEFAULT 100.00,     -- los macros estan por esta porcion
  kcal             DECIMAL(7,2)  NOT NULL,
  proteina_g       DECIMAL(6,2)  NOT NULL,
  carbohidrato_g   DECIMAL(6,2)  NOT NULL,
  grasa_g          DECIMAL(6,2)  NOT NULL,
  PRIMARY KEY (id_alimento),
  UNIQUE KEY uq_alimento_nombre (nombre),
  CONSTRAINT ck_alimento_valores CHECK (porcion_base_g > 0 AND kcal >= 0 AND proteina_g >= 0
                                        AND carbohidrato_g >= 0 AND grasa_g >= 0)
) ENGINE=InnoDB;

CREATE TABLE meta_nutricional (                               -- [Fase 1] macros diarios del cliente
  id_meta_nutricional  BIGINT       NOT NULL AUTO_INCREMENT,
  id_cliente           BIGINT       NOT NULL,
  kcal_diarias         DECIMAL(7,2) NOT NULL,
  proteina_g           DECIMAL(6,2) NOT NULL,
  carbohidrato_g       DECIMAL(6,2) NOT NULL,
  grasa_g              DECIMAL(6,2) NOT NULL,
  fecha_inicio         DATE         NOT NULL,
  fecha_fin            DATE         NULL,                     -- NULL = meta vigente
  PRIMARY KEY (id_meta_nutricional),
  KEY idx_meta_cliente (id_cliente, fecha_fin),
  CONSTRAINT fk_meta_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente)
) ENGINE=InnoDB;

CREATE TABLE registro_ingesta (                               -- [Fase 1] HU-13: un alimento consumido
  id_ingesta      BIGINT       NOT NULL AUTO_INCREMENT,
  id_cliente      BIGINT       NOT NULL,
  id_alimento     INT          NOT NULL,
  cantidad_g      DECIMAL(7,2) NOT NULL,
  fecha_hora      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,   -- el "dia" se obtiene de la fecha
  PRIMARY KEY (id_ingesta),
  KEY idx_ingesta_cliente_fecha (id_cliente, fecha_hora),
  CONSTRAINT fk_ingesta_cliente  FOREIGN KEY (id_cliente)  REFERENCES cliente (id_cliente),
  CONSTRAINT fk_ingesta_alimento FOREIGN KEY (id_alimento) REFERENCES alimento (id_alimento),
  CONSTRAINT ck_ingesta_cant CHECK (cantidad_g > 0)
) ENGINE=InnoDB;

-- =====================================================================
-- 4. ENTRENAMIENTO GENERADO POR IA (HU-11)                 [Fase 2]
--    Una rutina semanal activa por cliente; las anteriores quedan como historial (estado INACTIVO)
-- =====================================================================

CREATE TABLE grupo_muscular (
  id_grupo_muscular  INT         NOT NULL AUTO_INCREMENT,
  nombre             VARCHAR(50) NOT NULL,
  PRIMARY KEY (id_grupo_muscular),
  UNIQUE KEY uq_grupo_muscular_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE tipo_medicion (
  id_tipo_medicion  SMALLINT     NOT NULL AUTO_INCREMENT,
  nombre            VARCHAR(30)  NOT NULL,
  descripcion       VARCHAR(150) NULL,
  PRIMARY KEY (id_tipo_medicion),
  UNIQUE KEY uq_tipo_medicion_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE tipo_origen_ejercicio (
  id_tipo_origen_ejercicio  SMALLINT     NOT NULL AUTO_INCREMENT,
  nombre                    VARCHAR(30)  NOT NULL,
  descripcion               VARCHAR(150) NULL,
  PRIMARY KEY (id_tipo_origen_ejercicio),
  UNIQUE KEY uq_tipo_origen_ejercicio_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE ejercicio (
  id_ejercicio          INT          NOT NULL AUTO_INCREMENT,
  id_grupo_muscular     INT          NOT NULL,
  id_nivel_experiencia  INT          NOT NULL,       -- nivel minimo recomendado
  id_tipo_medicion      SMALLINT     NOT NULL DEFAULT 1,
  nombre                VARCHAR(100) NOT NULL,
  descripcion           VARCHAR(500) NULL,
  equipamiento          VARCHAR(100) NULL,           -- texto informativo, ej. "Banco y barra"
  imagen_ruta           VARCHAR(255) NULL,           -- imagen referencial guardada en el dispositivo
  id_tipo_estado        INT          NOT NULL DEFAULT 2,  -- 2 = ACTIVO
  PRIMARY KEY (id_ejercicio),
  UNIQUE KEY uq_ejercicio_nombre (nombre),
  CONSTRAINT fk_ejer_grupo FOREIGN KEY (id_grupo_muscular)    REFERENCES grupo_muscular (id_grupo_muscular),
  CONSTRAINT fk_ejer_nivel FOREIGN KEY (id_nivel_experiencia) REFERENCES nivel_experiencia (id_nivel_experiencia),
  CONSTRAINT fk_ejer_tipo_medicion FOREIGN KEY (id_tipo_medicion) REFERENCES tipo_medicion (id_tipo_medicion),
  CONSTRAINT fk_ejer_estado FOREIGN KEY (id_tipo_estado) REFERENCES tipo_estado (id_tipo_estado)
) ENGINE=InnoDB;

CREATE TABLE plan_entrenamiento (
  id_plan_entrenamiento  BIGINT      NOT NULL AUTO_INCREMENT,
  id_cliente             BIGINT      NOT NULL,
  id_nivel_experiencia   INT         NOT NULL,       -- nivel con el que se genero
  fecha_generacion       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_tipo_estado         INT         NOT NULL DEFAULT 2,  -- 2 = ACTIVO; INACTIVO conserva historial
  PRIMARY KEY (id_plan_entrenamiento),
  KEY idx_planent_cliente (id_cliente, id_tipo_estado),
  CONSTRAINT fk_planent_cliente  FOREIGN KEY (id_cliente)           REFERENCES cliente (id_cliente),
  CONSTRAINT fk_planent_nivel    FOREIGN KEY (id_nivel_experiencia) REFERENCES nivel_experiencia (id_nivel_experiencia),
  CONSTRAINT fk_planent_estado   FOREIGN KEY (id_tipo_estado)       REFERENCES tipo_estado (id_tipo_estado)
) ENGINE=InnoDB;

CREATE TABLE plan_entrenamiento_dia (
  id_plan_dia            BIGINT       NOT NULL AUTO_INCREMENT,
  id_plan_entrenamiento  BIGINT       NOT NULL,
  dia_semana             TINYINT      NOT NULL,
  hora_inicio            TIME         NOT NULL,
  hora_fin               TIME         NOT NULL,
  PRIMARY KEY (id_plan_dia),
  UNIQUE KEY uq_plandia (id_plan_entrenamiento, dia_semana),
  CONSTRAINT fk_plandia_plan FOREIGN KEY (id_plan_entrenamiento) REFERENCES plan_entrenamiento (id_plan_entrenamiento) ON DELETE CASCADE,
  CONSTRAINT ck_plandia_dia CHECK (dia_semana BETWEEN 1 AND 7),
  CONSTRAINT ck_plandia_horas CHECK (hora_fin > hora_inicio)
) ENGINE=InnoDB;

CREATE TABLE plan_entrenamiento_ejercicio (
  id_plan_ejercicio  BIGINT   NOT NULL AUTO_INCREMENT,
  id_plan_dia        BIGINT   NOT NULL,
  id_ejercicio       INT      NOT NULL,
  orden              TINYINT  NOT NULL,
  series             TINYINT  NOT NULL,
  repeticiones       TINYINT  NULL,                  -- si el ejercicio se mide en repeticiones
  segundos           SMALLINT NULL,                  -- si el ejercicio se mide en tiempo
  descanso_seg       SMALLINT NOT NULL,
  id_tipo_origen_ejercicio SMALLINT NOT NULL DEFAULT 1,  -- quien lo puso en la rutina
  id_tipo_estado     INT        NOT NULL DEFAULT 2,  -- 2 = ACTIVO; INACTIVO conserva historial
  PRIMARY KEY (id_plan_ejercicio),
  KEY idx_planejer_orden (id_plan_dia, orden),
  CONSTRAINT fk_planejer_dia  FOREIGN KEY (id_plan_dia)  REFERENCES plan_entrenamiento_dia (id_plan_dia) ON DELETE CASCADE,
  CONSTRAINT fk_planejer_ejer FOREIGN KEY (id_ejercicio) REFERENCES ejercicio (id_ejercicio),
  CONSTRAINT fk_planejer_tipo_origen FOREIGN KEY (id_tipo_origen_ejercicio) REFERENCES tipo_origen_ejercicio (id_tipo_origen_ejercicio),
  CONSTRAINT fk_planejer_estado FOREIGN KEY (id_tipo_estado) REFERENCES tipo_estado (id_tipo_estado),
  CONSTRAINT ck_planejer_medida CHECK (repeticiones IS NOT NULL OR segundos IS NOT NULL)
) ENGINE=InnoDB;

-- Registro de lo que el usuario realmente hizo, serie por serie (solo repeticiones o segundos, sin peso)
CREATE TABLE serie_realizada (
  id_serie_realizada  BIGINT   NOT NULL AUTO_INCREMENT,
  id_plan_ejercicio   BIGINT   NOT NULL,
  numero_serie        TINYINT  NOT NULL,
  repeticiones        TINYINT  NULL,
  segundos            SMALLINT NULL,
  fecha_hora          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_serie_realizada),
  KEY idx_serie_planejer_fecha (id_plan_ejercicio, fecha_hora),
  CONSTRAINT fk_serie_planejer FOREIGN KEY (id_plan_ejercicio) REFERENCES plan_entrenamiento_ejercicio (id_plan_ejercicio),
  CONSTRAINT ck_serie_medida CHECK (repeticiones IS NOT NULL OR segundos IS NOT NULL)
) ENGINE=InnoDB;

-- =====================================================================
-- 5. MEMBRESIAS (EP-01)                                    [Fase 2]
--    Sin suspensiones: la baja del cliente se maneja con cliente.id_tipo_estado = INACTIVO
-- =====================================================================

CREATE TABLE plan_membresia (
  id_plan_membresia  INT           NOT NULL AUTO_INCREMENT,
  nombre             VARCHAR(60)   NOT NULL,
  duracion_meses     TINYINT       NOT NULL,
  id_tipo_estado     INT           NOT NULL DEFAULT 2,  -- 2 = ACTIVO
  PRIMARY KEY (id_plan_membresia),
  UNIQUE KEY uq_plan_membresia_nombre (nombre),
  CONSTRAINT fk_planmem_estado FOREIGN KEY (id_tipo_estado) REFERENCES tipo_estado (id_tipo_estado),
  CONSTRAINT ck_planmem_valores CHECK (duracion_meses > 0)
) ENGINE=InnoDB;

-- Cada fila es un PERIODO pagado fuera de la app. Al renovar se inserta una fila nueva cuyo
-- fecha_inicio = MAX(hoy, vencimiento vigente + 1 dia), asi no se pierden los dias que le quedaban.
-- La vigencia no se guarda como estado: se calcula a partir de las fechas de los periodos.
CREATE TABLE membresia (
  id_membresia         BIGINT       NOT NULL AUTO_INCREMENT,
  id_cliente           BIGINT       NOT NULL,
  id_administrador     BIGINT       NOT NULL,
  id_plan_membresia    INT          NOT NULL,
  fecha_inicio         DATE         NOT NULL,
  fecha_vencimiento    DATE         NOT NULL,
  fecha_registro       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  observacion          VARCHAR(500) NULL,
  PRIMARY KEY (id_membresia),
  KEY idx_membresia_cliente (id_cliente, fecha_vencimiento),
  CONSTRAINT fk_mem_cliente FOREIGN KEY (id_cliente)          REFERENCES cliente (id_cliente),
  CONSTRAINT fk_mem_admin   FOREIGN KEY (id_administrador)    REFERENCES administrador (id_administrador),
  CONSTRAINT fk_mem_plan    FOREIGN KEY (id_plan_membresia)   REFERENCES plan_membresia (id_plan_membresia),
  CONSTRAINT ck_mem_fechas  CHECK (fecha_vencimiento >= fecha_inicio)
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 7. DATOS INICIALES DE CATALOGOS
-- =====================================================================

INSERT INTO tipo_estado (id_tipo_estado, nombre, descripcion) VALUES
  (1, 'PENDIENTE', 'Registrado, a la espera de validacion del administrador'),
  (2, 'ACTIVO',    'Registro habilitado para su uso'),
  (3, 'INACTIVO',  'Registro deshabilitado, conservado para historial');

INSERT INTO tipo_objetivo (nombre, descripcion) VALUES
  ('Perder grasa',        'Reducir porcentaje de grasa corporal'),
  ('Ganar masa muscular', 'Aumentar masa muscular'),
  ('Mantener peso',       'Mantener composicion corporal actual');

INSERT INTO nivel_experiencia (nombre, descripcion) VALUES
  ('Principiante', 'Menos de 6 meses entrenando'),
  ('Intermedio',   'Entre 6 meses y 2 anios entrenando'),
  ('Avanzado',     'Mas de 2 anios entrenando de forma constante');

INSERT INTO nivel_actividad (nombre, descripcion) VALUES
  ('Sedentario', 'Poco o ningun ejercicio'),
  ('Ligero',     'Ejercicio 1-3 dias por semana'),
  ('Moderado',   'Ejercicio 3-5 dias por semana'),
  ('Intenso',    'Ejercicio 6-7 dias por semana');

INSERT INTO grupo_muscular (nombre) VALUES
  ('Pecho'), ('Espalda'), ('Piernas'), ('Hombros'), ('Brazos'), ('Core'), ('Cardio');

INSERT INTO tipo_medicion (id_tipo_medicion, nombre, descripcion) VALUES
  (1, 'REPETICIONES', 'El ejercicio se registra por cantidad de repeticiones'),
  (2, 'TIEMPO', 'El ejercicio se registra por duración en segundos');

INSERT INTO tipo_origen_ejercicio (id_tipo_origen_ejercicio, nombre, descripcion) VALUES
  (1, 'IA', 'Ejercicio agregado por el generador de planes'),
  (2, 'USUARIO', 'Ejercicio agregado por el usuario');
