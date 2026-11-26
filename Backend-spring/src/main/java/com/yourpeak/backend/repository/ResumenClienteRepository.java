package com.yourpeak.backend.repository;

import com.yourpeak.backend.dto.ResumenNutricionalResponse.Meta;
import com.yourpeak.backend.dto.RutinaResponse.Actividad;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ResumenClienteRepository {
    private final JdbcClient jdbc;

    public ResumenClienteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Meta> buscarMeta(Long idCliente, LocalDate fecha) {
        return jdbc.sql("""
                SELECT kcal_diarias, proteina_g, carbohidrato_g, grasa_g
                FROM meta_nutricional
                WHERE id_cliente = :cliente AND fecha_inicio <= :fecha
                  AND (fecha_fin IS NULL OR fecha_fin >= :fecha)
                ORDER BY fecha_inicio DESC, id_meta_nutricional DESC
                LIMIT 1
                """)
                .param("cliente", idCliente).param("fecha", fecha)
                .query((rs, row) -> new Meta(rs.getBigDecimal("kcal_diarias"),
                        rs.getBigDecimal("proteina_g"), rs.getBigDecimal("carbohidrato_g"),
                        rs.getBigDecimal("grasa_g")))
                .optional();
    }

    public List<Actividad> listarActividadReciente(Long idCliente) {
        return jdbc.sql("""
                SELECT DATE(s.fecha_hora) AS fecha, COUNT(DISTINCT e.id_ejercicio) AS ejercicios,
                       CASE WHEN COUNT(DISTINCT g.id_grupo_muscular) = 1 THEN MIN(g.nombre)
                            ELSE 'Entrenamiento' END AS titulo
                FROM plan_entrenamiento p
                JOIN plan_entrenamiento_dia d ON d.id_plan_entrenamiento = p.id_plan_entrenamiento
                JOIN plan_entrenamiento_ejercicio pe ON pe.id_plan_dia = d.id_plan_dia
                JOIN serie_realizada s ON s.id_plan_ejercicio = pe.id_plan_ejercicio
                JOIN ejercicio e ON e.id_ejercicio = pe.id_ejercicio
                JOIN grupo_muscular g ON g.id_grupo_muscular = e.id_grupo_muscular
                WHERE p.id_cliente = :cliente
                GROUP BY DATE(s.fecha_hora)
                ORDER BY fecha DESC
                LIMIT 2
                """)
                .param("cliente", idCliente)
                .query((rs, row) -> new Actividad(rs.getDate("fecha").toLocalDate(),
                        rs.getString("titulo"), rs.getInt("ejercicios")))
                .list();
    }

    /**
     * Ejercicios del día de hoy en el plan activo que ya tienen series registradas hoy.
     * Se comparan por ejercicio y no por idPlanEjercicio porque guardar la rutina crea un plan nuevo
     * con otros identificadores; así lo marcado no se pierde si el cliente reordena y guarda.
     */
    public List<Long> listarEjerciciosRealizados(Long idCliente, java.time.LocalDate fecha) {
        return jdbc.sql("""
                SELECT DISTINCT pe.id_plan_ejercicio
                FROM plan_entrenamiento p
                JOIN tipo_estado ep ON ep.id_tipo_estado = p.id_tipo_estado AND UPPER(ep.nombre) = 'ACTIVO'
                JOIN plan_entrenamiento_dia d ON d.id_plan_entrenamiento = p.id_plan_entrenamiento
                JOIN plan_entrenamiento_ejercicio pe ON pe.id_plan_dia = d.id_plan_dia
                JOIN tipo_estado ee ON ee.id_tipo_estado = pe.id_tipo_estado AND UPPER(ee.nombre) = 'ACTIVO'
                WHERE p.id_cliente = :cliente AND d.dia_semana = :dia
                  AND pe.id_ejercicio IN (
                      SELECT pr.id_ejercicio
                      FROM serie_realizada s
                      JOIN plan_entrenamiento_ejercicio pr ON pr.id_plan_ejercicio = s.id_plan_ejercicio
                      JOIN plan_entrenamiento_dia dr ON dr.id_plan_dia = pr.id_plan_dia
                      JOIN plan_entrenamiento pp ON pp.id_plan_entrenamiento = dr.id_plan_entrenamiento
                      WHERE pp.id_cliente = :cliente AND s.fecha_hora >= :inicio AND s.fecha_hora < :fin)
                """)
                .param("cliente", idCliente)
                .param("dia", fecha.getDayOfWeek().getValue())
                .param("inicio", fecha.atStartOfDay())
                .param("fin", fecha.plusDays(1).atStartOfDay())
                .query((rs, row) -> rs.getLong(1))
                .list();
    }
}
