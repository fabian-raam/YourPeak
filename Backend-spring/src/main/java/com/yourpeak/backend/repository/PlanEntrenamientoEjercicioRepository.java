package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.PlanEntrenamientoEjercicio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanEntrenamientoEjercicioRepository extends JpaRepository<PlanEntrenamientoEjercicio, Long> {
    @EntityGraph(attributePaths = {"ejercicio", "ejercicio.grupoMuscular", "ejercicio.tipoMedicion", "tipoOrigen"})
    List<PlanEntrenamientoEjercicio> findAllByPlanDia_IdPlanDiaOrderByOrdenAsc(Long idPlanDia);
}
