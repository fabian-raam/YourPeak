-- Agrega el dato requerido por la formula de calorias al perfil del cliente.
-- Ejecutar manualmente una sola vez sobre yourpeak_db.
-- Los clientes existentes conservaran sexo = NULL hasta que completen el dato.

USE yourpeak_db;

ALTER TABLE cliente
    ADD COLUMN sexo VARCHAR(10) NULL AFTER id_nivel_actividad,
    ADD CONSTRAINT ck_cliente_sexo
        CHECK (sexo IS NULL OR sexo IN ('HOMBRE', 'MUJER'));
