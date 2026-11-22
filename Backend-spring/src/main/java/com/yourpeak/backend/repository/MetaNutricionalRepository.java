package com.yourpeak.backend.repository;

import com.yourpeak.backend.dto.ResumenNutricionalResponse.Meta;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/** Persiste metas calculadas y conserva sus periodos para consultar el historial. */
@Repository
public class MetaNutricionalRepository {
    private final JdbcClient jdbc;

    public MetaNutricionalRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Meta> buscarVigente(Long idCliente, LocalDate fecha) {
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

    /** Elimina metas futuras obsoletas antes de programar el cálculo actualizado. */
    public void eliminarMetasFuturas(Long idCliente, LocalDate fechaActual) {
        jdbc.sql("""
                DELETE FROM meta_nutricional
                WHERE id_cliente = :cliente AND fecha_inicio > :fechaActual
                """)
                .param("cliente", idCliente).param("fechaActual", fechaActual)
                .update();
    }

    /** Cierra el periodo que se superpone con el inicio de la nueva meta. */
    public void cerrarMetasAnteriores(Long idCliente, LocalDate fechaInicioNueva) {
        jdbc.sql("""
                UPDATE meta_nutricional
                SET fecha_fin = :fechaFin
                WHERE id_cliente = :cliente AND fecha_inicio < :fechaInicio
                  AND (fecha_fin IS NULL OR fecha_fin >= :fechaInicio)
                """)
                .param("cliente", idCliente)
                .param("fechaInicio", fechaInicioNueva)
                .param("fechaFin", fechaInicioNueva.minusDays(1))
                .update();
    }

    public void guardar(Long idCliente, Meta meta, LocalDate fechaInicio) {
        jdbc.sql("""
                INSERT INTO meta_nutricional
                    (id_cliente, kcal_diarias, proteina_g, carbohidrato_g,
                     grasa_g, fecha_inicio, fecha_fin)
                VALUES (:cliente, :kcal, :proteina, :carbohidrato, :grasa, :inicio, NULL)
                """)
                .param("cliente", idCliente)
                .param("kcal", meta.kcal())
                .param("proteina", meta.proteinaG())
                .param("carbohidrato", meta.carbohidratoG())
                .param("grasa", meta.grasaG())
                .param("inicio", fechaInicio)
                .update();
    }
}
