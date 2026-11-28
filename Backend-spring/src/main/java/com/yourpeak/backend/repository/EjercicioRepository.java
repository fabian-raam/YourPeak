package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.Ejercicio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EjercicioRepository extends JpaRepository<Ejercicio, Integer> {
    @EntityGraph(attributePaths = {"grupoMuscular", "nivelExperiencia", "tipoMedicion", "tipoEstado"})
    List<Ejercicio> findAllByOrderByNombreAsc();

    @Override
    @EntityGraph(attributePaths = {"grupoMuscular", "nivelExperiencia", "tipoMedicion", "tipoEstado"})
    Optional<Ejercicio> findById(Integer idEjercicio);

    // MariaDB no admite el FOR UPDATE OF que Hibernate genera con el dialecto MySQL.
    @Query(value = "SELECT * FROM ejercicio WHERE id_ejercicio = :id FOR UPDATE", nativeQuery = true)
    Optional<Ejercicio> buscarParaEditar(@Param("id") Integer idEjercicio);

    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdEjercicioNot(String nombre, Integer idEjercicio);
}
