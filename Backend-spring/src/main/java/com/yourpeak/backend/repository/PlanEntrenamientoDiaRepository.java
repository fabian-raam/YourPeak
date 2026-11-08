package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.PlanEntrenamientoDia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanEntrenamientoDiaRepository extends JpaRepository<PlanEntrenamientoDia, Long> {
    List<PlanEntrenamientoDia> findAllByPlanEntrenamiento_IdPlanEntrenamientoOrderByDiaSemanaAsc(
            Long idPlanEntrenamiento
    );
}
