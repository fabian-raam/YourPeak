package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.TipoEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TipoEstadoRepository
        extends JpaRepository<TipoEstado, Integer> {

    Optional<TipoEstado> findByNombre(String nombre);
}