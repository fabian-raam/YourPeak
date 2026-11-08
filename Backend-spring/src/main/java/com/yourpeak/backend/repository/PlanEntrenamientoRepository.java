package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.PlanEntrenamiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanEntrenamientoRepository extends JpaRepository<PlanEntrenamiento, Long> {
    @EntityGraph(attributePaths = {"tipoEstado", "nivelExperiencia"})
    Optional<PlanEntrenamiento> findFirstByCliente_IdClienteAndTipoEstado_NombreIgnoreCaseOrderByFechaGeneracionDesc(
            Long idCliente, String estado
    );
}
