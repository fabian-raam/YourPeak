package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepository
        extends JpaRepository<Administrador, Long> {

    boolean existsByUsuario_IdUsuario(Long idUsuario);
}
