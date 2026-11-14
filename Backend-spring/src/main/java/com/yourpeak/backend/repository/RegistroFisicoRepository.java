package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.RegistroFisico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RegistroFisicoRepository extends JpaRepository<RegistroFisico, Long> {
    Optional<RegistroFisico> findFirstByCliente_IdClienteOrderByFechaRegistroDesc(Long idCliente);
}
