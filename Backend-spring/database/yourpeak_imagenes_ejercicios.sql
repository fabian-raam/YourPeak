-- Asocia el catálogo existente con las ilustraciones del detalle de ejercicios.
-- imagen_ruta guarda una clave (no el archivo JPG). Las imágenes están en
-- Mobile/app/src/main/res/drawable-nodpi y en Frontend-web/react/src/assets/exercises.
-- Ejecutar una sola vez después de cargar el catálogo en yourpeak_db.

USE yourpeak_db;
SET NAMES utf8mb4;

UPDATE ejercicio
SET imagen_ruta = CASE nombre
    WHEN 'Aperturas con mancuernas' THEN 'aperturas-con-mancuernas'
    WHEN 'Curl de bíceps' THEN 'curl-de-biceps'
    WHEN 'Elevaciones laterales' THEN 'elevaciones-laterales'
    WHEN 'Extensión de tríceps en polea' THEN 'extension-de-triceps-en-polea'
    WHEN 'Flexiones de pecho' THEN 'flexiones-de-pecho'
    WHEN 'Jalón al pecho' THEN 'jalon-al-pecho'
    WHEN 'Peso muerto rumano' THEN 'peso-muerto-rumano'
    WHEN 'Plancha frontal' THEN 'plancha-frontal'
    WHEN 'Prensa de piernas' THEN 'prensa-de-piernas'
    WHEN 'Press de banca' THEN 'press-de-banca'
    WHEN 'Press inclinado con mancuernas' THEN 'press-inclinado-con-mancuernas'
    WHEN 'Press militar' THEN 'press-militar'
    WHEN 'Remo con mancuerna' THEN 'remo-con-mancuerna'
    WHEN 'Saltos de tijera' THEN 'saltos-de-tijera'
    WHEN 'Sentadilla goblet' THEN 'sentadilla-goblet'
    WHEN 'Zancadas caminando' THEN 'zancadas-caminando'
    ELSE imagen_ruta
END
WHERE nombre IN (
    'Aperturas con mancuernas', 'Curl de bíceps', 'Elevaciones laterales',
    'Extensión de tríceps en polea', 'Flexiones de pecho', 'Jalón al pecho',
    'Peso muerto rumano', 'Plancha frontal', 'Prensa de piernas', 'Press de banca',
    'Press inclinado con mancuernas', 'Press militar', 'Remo con mancuerna',
    'Saltos de tijera', 'Sentadilla goblet', 'Zancadas caminando'
);

SELECT nombre, imagen_ruta
FROM ejercicio
WHERE imagen_ruta IS NOT NULL
ORDER BY nombre;
