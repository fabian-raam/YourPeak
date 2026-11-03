-- Datos iniciales de demostración para HU-22 y HU-23.
-- Ejecutar después de yourpeak_schema.sql en MySQL/MariaDB.
-- Se insertan 16 ejercicios y 32 alimentos; la pantalla de alimentos muestra 8 por página.
-- Los nombres existentes se conservan para evitar duplicados y no sobrescribir datos editados.

USE yourpeak_db;
SET NAMES utf8mb4;

-- Valores nutricionales de referencia para demostración, correspondientes a cada porción indicada.
-- Pueden variar según marca, preparación y tipo de alimento.
INSERT INTO alimento
    (nombre, porcion_base_g, kcal, proteina_g, carbohidrato_g, grasa_g)
SELECT inicial.nombre, inicial.porcion_base_g, inicial.kcal,
       inicial.proteina_g, inicial.carbohidrato_g, inicial.grasa_g
FROM (
    SELECT 'Avena en hojuelas' AS nombre, 40.00 AS porcion_base_g, 152.00 AS kcal,
           5.30 AS proteina_g, 27.10 AS carbohidrato_g, 2.70 AS grasa_g
    UNION ALL SELECT 'Arroz integral cocido', 100.00, 123.00, 2.70, 25.60, 1.00
    UNION ALL SELECT 'Pechuga de pollo cocida', 100.00, 165.00, 31.00, 0.00, 3.60
    UNION ALL SELECT 'Huevo sancochado', 50.00, 78.00, 6.30, 0.60, 5.30
    UNION ALL SELECT 'Plátano', 100.00, 89.00, 1.10, 22.80, 0.30
    UNION ALL SELECT 'Manzana con cáscara', 100.00, 52.00, 0.30, 13.80, 0.20
    UNION ALL SELECT 'Quinua cocida', 100.00, 120.00, 4.40, 21.30, 1.90
    UNION ALL SELECT 'Yogur griego natural', 100.00, 59.00, 10.20, 3.60, 0.40
    UNION ALL SELECT 'Lentejas cocidas', 100.00, 116.00, 9.00, 20.10, 0.40
    UNION ALL SELECT 'Palta', 100.00, 160.00, 2.00, 8.50, 14.70
    UNION ALL SELECT 'Atún en agua escurrido', 100.00, 116.00, 25.50, 0.00, 0.80
    UNION ALL SELECT 'Camote cocido', 100.00, 86.00, 1.60, 20.10, 0.10
    UNION ALL SELECT 'Brócoli cocido', 100.00, 35.00, 2.40, 7.20, 0.40
    UNION ALL SELECT 'Leche descremada', 240.00, 83.00, 8.30, 12.20, 0.20
    UNION ALL SELECT 'Pan integral', 28.00, 69.00, 3.60, 11.60, 1.10
    UNION ALL SELECT 'Almendras', 28.00, 162.00, 6.00, 6.00, 14.00
    UNION ALL SELECT 'Papa cocida', 100.00, 87.00, 1.90, 20.10, 0.10
    UNION ALL SELECT 'Fresas', 100.00, 32.00, 0.70, 7.70, 0.30
    UNION ALL SELECT 'Naranja', 100.00, 47.00, 0.90, 11.80, 0.10
    UNION ALL SELECT 'Mango', 100.00, 60.00, 0.80, 15.00, 0.40
    UNION ALL SELECT 'Mantequilla de maní', 32.00, 188.00, 8.00, 6.00, 16.00
    UNION ALL SELECT 'Queso fresco', 30.00, 79.00, 5.50, 0.90, 6.10
    UNION ALL SELECT 'Tofu firme', 100.00, 144.00, 17.30, 2.80, 8.70
    UNION ALL SELECT 'Garbanzos cocidos', 100.00, 164.00, 8.90, 27.40, 2.60
    UNION ALL SELECT 'Frijoles negros cocidos', 100.00, 132.00, 8.90, 23.70, 0.50
    UNION ALL SELECT 'Pasta integral cocida', 100.00, 124.00, 5.30, 26.50, 0.50
    UNION ALL SELECT 'Salmón cocido', 100.00, 206.00, 22.10, 0.00, 12.40
    UNION ALL SELECT 'Carne de res magra cocida', 100.00, 217.00, 26.00, 0.00, 11.80
    UNION ALL SELECT 'Camarones cocidos', 100.00, 99.00, 24.00, 0.20, 0.30
    UNION ALL SELECT 'Zanahoria cocida', 100.00, 35.00, 0.80, 8.20, 0.20
    UNION ALL SELECT 'Espinaca cocida', 100.00, 23.00, 3.00, 3.80, 0.30
    UNION ALL SELECT 'Aceite de oliva', 10.00, 88.00, 0.00, 0.00, 10.00
) AS inicial
WHERE NOT EXISTS (
    SELECT 1 FROM alimento existente WHERE existente.nombre = inicial.nombre
);

-- El ejercicio toma sus IDs de los catálogos por nombre para no depender
-- de que los IDs autoincrementales sean iguales en todos los equipos.
INSERT INTO ejercicio
    (id_grupo_muscular, id_nivel_experiencia, id_tipo_medicion,
     nombre, descripcion, equipamiento, imagen_ruta, id_tipo_estado)
SELECT grupo.id_grupo_muscular, nivel.id_nivel_experiencia, medicion.id_tipo_medicion,
       inicial.nombre, inicial.descripcion, inicial.equipamiento, NULL, estado.id_tipo_estado
FROM (
    SELECT 'Pecho' AS grupo, 'Principiante' AS nivel, 'REPETICIONES' AS medicion,
           'Press de banca' AS nombre, 'Empuja la barra desde el pecho manteniendo los pies apoyados.' AS descripcion,
           'Barra y banco' AS equipamiento
    UNION ALL SELECT 'Pecho', 'Principiante', 'REPETICIONES', 'Press inclinado con mancuernas',
           'Empuja las mancuernas desde un banco inclinado.', 'Mancuernas y banco inclinado'
    UNION ALL SELECT 'Pecho', 'Principiante', 'REPETICIONES', 'Flexiones de pecho',
           'Flexiona los brazos y acerca el pecho al suelo manteniendo el cuerpo alineado.', 'Peso corporal'
    UNION ALL SELECT 'Pecho', 'Intermedio', 'REPETICIONES', 'Aperturas con mancuernas',
           'Abre y cierra los brazos con una ligera flexión de codos.', 'Mancuernas y banco'
    UNION ALL SELECT 'Espalda', 'Principiante', 'REPETICIONES', 'Jalón al pecho',
           'Lleva la barra hacia la parte superior del pecho con control.', 'Polea alta y barra'
    UNION ALL SELECT 'Espalda', 'Principiante', 'REPETICIONES', 'Remo con mancuerna',
           'Lleva la mancuerna hacia la cadera manteniendo la espalda estable.', 'Mancuerna y banco'
    UNION ALL SELECT 'Espalda', 'Intermedio', 'REPETICIONES', 'Peso muerto rumano',
           'Lleva la cadera hacia atrás y baja la carga manteniendo la espalda neutra.', 'Barra o mancuernas'
    UNION ALL SELECT 'Piernas', 'Principiante', 'REPETICIONES', 'Sentadilla goblet',
           'Desciende con una mancuerna frente al pecho y vuelve a subir.', 'Mancuerna o pesa rusa'
    UNION ALL SELECT 'Piernas', 'Principiante', 'REPETICIONES', 'Prensa de piernas',
           'Empuja la plataforma sin bloquear las rodillas al extender.', 'Máquina de prensa'
    UNION ALL SELECT 'Piernas', 'Intermedio', 'REPETICIONES', 'Zancadas caminando',
           'Da pasos largos alternando las piernas y controla el descenso.', 'Mancuernas opcionales'
    UNION ALL SELECT 'Hombros', 'Principiante', 'REPETICIONES', 'Elevaciones laterales',
           'Eleva los brazos hacia los lados hasta la altura de los hombros.', 'Mancuernas'
    UNION ALL SELECT 'Hombros', 'Intermedio', 'REPETICIONES', 'Press militar',
           'Empuja la carga sobre la cabeza manteniendo el tronco firme.', 'Barra o mancuernas'
    UNION ALL SELECT 'Brazos', 'Principiante', 'REPETICIONES', 'Curl de bíceps',
           'Flexiona los codos para acercar las mancuernas a los hombros.', 'Mancuernas'
    UNION ALL SELECT 'Brazos', 'Principiante', 'REPETICIONES', 'Extensión de tríceps en polea',
           'Extiende los codos hacia abajo manteniéndolos cerca del cuerpo.', 'Polea y cuerda'
    UNION ALL SELECT 'Core', 'Principiante', 'TIEMPO', 'Plancha frontal',
           'Mantén el cuerpo alineado apoyado en antebrazos y pies.', 'Peso corporal'
    UNION ALL SELECT 'Cardio', 'Principiante', 'TIEMPO', 'Saltos de tijera',
           'Alterna apertura y cierre de piernas y brazos con un ritmo controlado.', 'Peso corporal'
) AS inicial
JOIN grupo_muscular grupo ON grupo.nombre = inicial.grupo
JOIN nivel_experiencia nivel ON nivel.nombre = inicial.nivel
JOIN tipo_medicion medicion ON medicion.nombre = inicial.medicion
JOIN tipo_estado estado ON estado.nombre = 'ACTIVO'
WHERE NOT EXISTS (
    SELECT 1 FROM ejercicio existente WHERE existente.nombre = inicial.nombre
);

-- Resumen de los registros cargados con esta lista de nombres.
SELECT COUNT(*) AS alimentos_iniciales
FROM alimento
WHERE nombre IN (
    'Avena en hojuelas', 'Arroz integral cocido', 'Pechuga de pollo cocida', 'Huevo sancochado',
    'Plátano', 'Manzana con cáscara', 'Quinua cocida', 'Yogur griego natural',
    'Lentejas cocidas', 'Palta', 'Atún en agua escurrido', 'Camote cocido',
    'Brócoli cocido', 'Leche descremada', 'Pan integral', 'Almendras',
    'Papa cocida', 'Fresas', 'Naranja', 'Mango', 'Mantequilla de maní', 'Queso fresco', 'Tofu firme',
    'Garbanzos cocidos', 'Frijoles negros cocidos', 'Pasta integral cocida', 'Salmón cocido',
    'Carne de res magra cocida', 'Camarones cocidos', 'Zanahoria cocida', 'Espinaca cocida',
    'Aceite de oliva'
);

SELECT COUNT(*) AS ejercicios_iniciales
FROM ejercicio
WHERE nombre IN (
    'Press de banca', 'Press inclinado con mancuernas', 'Flexiones de pecho', 'Aperturas con mancuernas',
    'Jalón al pecho', 'Remo con mancuerna', 'Peso muerto rumano', 'Sentadilla goblet',
    'Prensa de piernas', 'Zancadas caminando', 'Elevaciones laterales', 'Press militar',
    'Curl de bíceps', 'Extensión de tríceps en polea', 'Plancha frontal', 'Saltos de tijera'
);
