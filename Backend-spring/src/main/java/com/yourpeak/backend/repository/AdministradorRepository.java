package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepository
        extends JpaRepository<Administrador, Long> {

    boolean existsByUsuario_IdUsuario(Long idUsuario);

    Optional<Administrador> findByUsuario_IdUsuario(Long idUsuario);
}
