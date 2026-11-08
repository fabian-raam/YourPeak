package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.TipoOrigenEjercicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoOrigenEjercicioRepository extends JpaRepository<TipoOrigenEjercicio, Short> {
    Optional<TipoOrigenEjercicio> findByNombreIgnoreCase(String nombre);
}
