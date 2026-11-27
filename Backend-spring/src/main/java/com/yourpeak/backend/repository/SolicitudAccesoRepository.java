package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.SolicitudAcceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SolicitudAccesoRepository extends JpaRepository<SolicitudAcceso, Long> {

    List<SolicitudAcceso> findAllByCliente_IdClienteOrderByFechaSolicitudDescIdSolicitudAccesoDesc(Long idCliente);

    Optional<SolicitudAcceso> findFirstByCliente_IdClienteOrderByFechaSolicitudDescIdSolicitudAccesoDesc(Long idCliente);

    long countByCliente_IdClienteAndFechaSolicitudGreaterThanEqual(Long idCliente, LocalDateTime desde);
}
