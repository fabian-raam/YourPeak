package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.PlanMembresia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanMembresiaRepository extends JpaRepository<PlanMembresia, Integer> {

    @EntityGraph(attributePaths = "tipoEstado")
    List<PlanMembresia> findAllByOrderByNombreAsc();

    @Override
    @EntityGraph(attributePaths = "tipoEstado")
    Optional<PlanMembresia> findById(Integer idPlanMembresia);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdPlanMembresiaNot(String nombre, Integer idPlanMembresia);
}
