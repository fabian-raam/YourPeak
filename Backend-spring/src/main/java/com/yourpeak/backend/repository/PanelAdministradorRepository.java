package com.yourpeak.backend.repository;

import com.yourpeak.backend.dto.DetalleEntrenamientoAdministradorResponse;
import com.yourpeak.backend.dto.EntrenamientoAdministradorResponse;
import com.yourpeak.backend.dto.ResumenPanelAdministradorResponse;
import com.yourpeak.backend.dto.RutinaResponse;
import com.yourpeak.backend.dto.DiaRutinaResponse;
import com.yourpeak.backend.dto.EjercicioRutinaResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Consultas de lectura para el inicio y la sección de entrenamientos del administrador. */
@Repository
public class PanelAdministradorRepository {
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private final JdbcClient jdbc;

    public PanelAdministradorRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public ResumenPanelAdministradorResponse consultarResumen() {
        return jdbc.sql("""
                SELECT
                    (SELECT COUNT(*) FROM cliente c JOIN tipo_estado e ON e.id_tipo_estado = c.id_tipo_estado
                     WHERE e.nombre = 'ACTIVO' AND NOT EXISTS
                       (SELECT 1 FROM administrador a WHERE a.id_usuario = c.id_usuario)) AS clientes_activos,
                    (SELECT COUNT(*) FROM cliente c JOIN tipo_estado e ON e.id_tipo_estado = c.id_tipo_estado
                     WHERE e.nombre = 'PENDIENTE' AND NOT EXISTS
                       (SELECT 1 FROM administrador a WHERE a.id_usuario = c.id_usuario)) AS solicitudes_pendientes,
                    (SELECT COUNT(*) FROM cliente c JOIN tipo_estado e ON e.id_tipo_estado = c.id_tipo_estado
                     WHERE e.nombre = 'INACTIVO' AND NOT EXISTS
                       (SELECT 1 FROM administrador a WHERE a.id_usuario = c.id_usuario)) AS clientes_inactivos,
                    (SELECT COUNT(*) FROM plan_membresia p JOIN tipo_estado e ON e.id_tipo_estado = p.id_tipo_estado
                     WHERE e.nombre = 'ACTIVO') AS planes_membresia_activos,
                    (SELECT COUNT(*) FROM ejercicio x JOIN tipo_estado e ON e.id_tipo_estado = x.id_tipo_estado
                     WHERE e.nombre = 'ACTIVO') AS ejercicios_activos,
                    (SELECT COUNT(*) FROM alimento) AS alimentos_catalogo
                """)
                .query((rs, row) -> new ResumenPanelAdministradorResponse(
                        rs.getLong("clientes_activos"),
                        rs.getLong("solicitudes_pendientes"),
                        rs.getLong("clientes_inactivos"),
                        rs.getLong("planes_membresia_activos"),
                        rs.getLong("ejercicios_activos"),
                        rs.getLong("alimentos_catalogo")
                )).single();
    }

    public List<EntrenamientoAdministradorResponse> listarEntrenamientos() {
        LocalDateTime desde = LocalDate.now(ZONA_LIMA).minusDays(29).atStartOfDay();
        return jdbc.sql("""
                SELECT c.id_cliente, u.nombre, u.apellido, u.correo, estado.nombre AS estado,
                       rutina.fecha_generacion AS fecha_rutina,
                       COALESCE(rutina.dias_programados, 0) AS dias_programados,
                       COALESCE(rutina.ejercicios_programados, 0) AS ejercicios_programados,
                       COALESCE(actividad.sesiones, 0) AS sesiones_ultimos_30_dias,
                       actividad.ultima_actividad
                FROM cliente c
                JOIN usuario u ON u.id_usuario = c.id_usuario
                JOIN tipo_estado estado ON estado.id_tipo_estado = c.id_tipo_estado
                LEFT JOIN (
                    SELECT p.id_cliente, MAX(p.fecha_generacion) AS fecha_generacion,
                           COUNT(DISTINCT d.id_plan_dia) AS dias_programados,
                           COUNT(DISTINCT pe.id_plan_ejercicio) AS ejercicios_programados
                    FROM plan_entrenamiento p
                    JOIN tipo_estado ep ON ep.id_tipo_estado = p.id_tipo_estado AND ep.nombre = 'ACTIVO'
                    LEFT JOIN plan_entrenamiento_dia d ON d.id_plan_entrenamiento = p.id_plan_entrenamiento
                    LEFT JOIN plan_entrenamiento_ejercicio pe ON pe.id_plan_dia = d.id_plan_dia
                    GROUP BY p.id_cliente
                ) rutina ON rutina.id_cliente = c.id_cliente
                LEFT JOIN (
                    SELECT p.id_cliente, COUNT(DISTINCT DATE(s.fecha_hora)) AS sesiones,
                           MAX(s.fecha_hora) AS ultima_actividad
                    FROM plan_entrenamiento p
                    JOIN plan_entrenamiento_dia d ON d.id_plan_entrenamiento = p.id_plan_entrenamiento
                    JOIN plan_entrenamiento_ejercicio pe ON pe.id_plan_dia = d.id_plan_dia
                    JOIN serie_realizada s ON s.id_plan_ejercicio = pe.id_plan_ejercicio
                    WHERE s.fecha_hora >= :desde
                    GROUP BY p.id_cliente
                ) actividad ON actividad.id_cliente = c.id_cliente
                WHERE estado.nombre IN ('ACTIVO', 'INACTIVO')
                  AND NOT EXISTS (SELECT 1 FROM administrador a WHERE a.id_usuario = c.id_usuario)
                ORDER BY u.apellido, u.nombre
                """)
                .param("desde", desde)
                .query((rs, row) -> new EntrenamientoAdministradorResponse(
                        rs.getLong("id_cliente"), rs.getString("nombre"), rs.getString("apellido"),
                        rs.getString("correo"), rs.getString("estado"),
                        rs.getObject("fecha_rutina", LocalDateTime.class),
                        rs.getInt("dias_programados"), rs.getInt("ejercicios_programados"),
                        rs.getInt("sesiones_ultimos_30_dias"),
                        rs.getObject("ultima_actividad", LocalDateTime.class)
                )).list();
    }

    public Optional<DetalleEntrenamientoAdministradorResponse> consultarEntrenamiento(Long idCliente) {
        LocalDateTime desde = LocalDate.now(ZONA_LIMA).minusDays(29).atStartOfDay();
        Optional<IdentidadCliente> identidad = jdbc.sql("""
                SELECT c.id_cliente, u.nombre, u.apellido, u.correo, estado.nombre AS estado,
                       rutina.fecha_generacion AS fecha_rutina
                FROM cliente c
                JOIN usuario u ON u.id_usuario = c.id_usuario
                JOIN tipo_estado estado ON estado.id_tipo_estado = c.id_tipo_estado
                LEFT JOIN (
                    SELECT p.id_cliente, MAX(p.fecha_generacion) AS fecha_generacion
                    FROM plan_entrenamiento p
                    JOIN tipo_estado ep ON ep.id_tipo_estado = p.id_tipo_estado AND ep.nombre = 'ACTIVO'
                    GROUP BY p.id_cliente
                ) rutina ON rutina.id_cliente = c.id_cliente
                WHERE c.id_cliente = :cliente
                  AND estado.nombre IN ('ACTIVO', 'INACTIVO')
                  AND NOT EXISTS (SELECT 1 FROM administrador a WHERE a.id_usuario = c.id_usuario)
                """)
                .param("cliente", idCliente)
                .query((rs, row) -> new IdentidadCliente(rs.getLong("id_cliente"), rs.getString("nombre"),
                        rs.getString("apellido"), rs.getString("correo"), rs.getString("estado"),
                        rs.getObject("fecha_rutina", LocalDateTime.class)))
                .optional();

        if (identidad.isEmpty()) return Optional.empty();

        Map<Integer, List<EjercicioRutinaResponse>> ejerciciosPorDia = new LinkedHashMap<>();
        Map<Integer, HorarioDia> horarios = new LinkedHashMap<>();
        if (identidad.get().fechaRutina() != null) {
            jdbc.sql("""
                    SELECT d.dia_semana, d.hora_inicio, d.hora_fin,
                           pe.id_plan_ejercicio, e.id_ejercicio, e.nombre AS ejercicio,
                           gm.nombre AS grupo, tm.nombre AS medicion, pe.orden, pe.series,
                           pe.repeticiones, pe.segundos, pe.descanso_seg,
                           origen.nombre AS origen, e.imagen_ruta
                    FROM plan_entrenamiento p
                    JOIN tipo_estado ep ON ep.id_tipo_estado = p.id_tipo_estado AND ep.nombre = 'ACTIVO'
                    JOIN (
                        SELECT p2.id_plan_entrenamiento
                        FROM plan_entrenamiento p2
                        JOIN tipo_estado ep2 ON ep2.id_tipo_estado = p2.id_tipo_estado
                        WHERE p2.id_cliente = :cliente AND ep2.nombre = 'ACTIVO'
                        ORDER BY p2.fecha_generacion DESC, p2.id_plan_entrenamiento DESC
                        LIMIT 1
                    ) actual ON actual.id_plan_entrenamiento = p.id_plan_entrenamiento
                    JOIN plan_entrenamiento_dia d ON d.id_plan_entrenamiento = p.id_plan_entrenamiento
                    JOIN plan_entrenamiento_ejercicio pe ON pe.id_plan_dia = d.id_plan_dia
                    JOIN ejercicio e ON e.id_ejercicio = pe.id_ejercicio
                    JOIN grupo_muscular gm ON gm.id_grupo_muscular = e.id_grupo_muscular
                    JOIN tipo_medicion tm ON tm.id_tipo_medicion = e.id_tipo_medicion
                    JOIN tipo_origen_ejercicio origen ON origen.id_tipo_origen_ejercicio = pe.id_tipo_origen_ejercicio
                    WHERE p.id_cliente = :cliente
                    ORDER BY p.fecha_generacion DESC, d.dia_semana, pe.orden
                    """)
                    .param("cliente", idCliente)
                    .query((rs, row) -> {
                        int dia = rs.getInt("dia_semana");
                        horarios.putIfAbsent(dia, new HorarioDia(rs.getObject("hora_inicio", java.time.LocalTime.class),
                                rs.getObject("hora_fin", java.time.LocalTime.class)));
                        Object repeticiones = rs.getObject("repeticiones");
                        Object segundos = rs.getObject("segundos");
                        ejerciciosPorDia.computeIfAbsent(dia, key -> new ArrayList<>()).add(
                                new EjercicioRutinaResponse(rs.getLong("id_plan_ejercicio"),
                                        rs.getInt("id_ejercicio"), rs.getString("ejercicio"), rs.getString("grupo"),
                                        rs.getString("medicion"), rs.getInt("orden"), rs.getInt("series"),
                                        repeticiones == null ? null : ((Number) repeticiones).intValue(),
                                        segundos == null ? null : ((Number) segundos).intValue(),
                                        rs.getInt("descanso_seg"), rs.getString("origen"), rs.getString("imagen_ruta")));
                        return dia;
                    }).list();
        }

        List<DiaRutinaResponse> dias = new ArrayList<>();
        horarios.forEach((dia, horario) -> dias.add(new DiaRutinaResponse(dia, true,
                horario.inicio(), horario.fin(), ejerciciosPorDia.getOrDefault(dia, List.of()))));

        List<RutinaResponse.Actividad> actividad = jdbc.sql("""
                SELECT DATE(s.fecha_hora) AS fecha,
                       COUNT(DISTINCT e.id_ejercicio) AS ejercicios,
                       CASE WHEN COUNT(DISTINCT g.id_grupo_muscular) = 1 THEN MIN(g.nombre)
                            ELSE 'Entrenamiento' END AS titulo
                FROM plan_entrenamiento p
                JOIN plan_entrenamiento_dia d ON d.id_plan_entrenamiento = p.id_plan_entrenamiento
                JOIN plan_entrenamiento_ejercicio pe ON pe.id_plan_dia = d.id_plan_dia
                JOIN serie_realizada s ON s.id_plan_ejercicio = pe.id_plan_ejercicio
                JOIN ejercicio e ON e.id_ejercicio = pe.id_ejercicio
                JOIN grupo_muscular g ON g.id_grupo_muscular = e.id_grupo_muscular
                WHERE p.id_cliente = :cliente AND s.fecha_hora >= :desde
                GROUP BY DATE(s.fecha_hora)
                ORDER BY fecha DESC
                LIMIT 10
                """)
                .param("cliente", idCliente).param("desde", desde)
                .query((rs, row) -> new RutinaResponse.Actividad(
                        rs.getDate("fecha").toLocalDate(), rs.getString("titulo"), rs.getInt("ejercicios")))
                .list();

        IdentidadCliente cliente = identidad.get();
        return Optional.of(new DetalleEntrenamientoAdministradorResponse(cliente.idCliente(), cliente.nombre(),
                cliente.apellido(), cliente.correo(), cliente.estado(), cliente.fechaRutina(), dias, actividad));
    }

    private record IdentidadCliente(Long idCliente, String nombre, String apellido, String correo,
                                    String estado, LocalDateTime fechaRutina) { }
    private record HorarioDia(java.time.LocalTime inicio, java.time.LocalTime fin) { }
}
