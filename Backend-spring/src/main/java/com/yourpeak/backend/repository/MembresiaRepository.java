package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.Membresia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.time.LocalDate;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    /**
     * Devuelve el período del cliente que termina más tarde.
     * Sirve para calcular una renovación sin perder días ya pagados.
     */
    Optional<Membresia> findFirstByCliente_IdClienteOrderByFechaVencimientoDesc(
            Long idCliente
    );

    List<Membresia> findAllByCliente_IdClienteOrderByFechaInicioDesc(Long idCliente);

    boolean existsByCliente_IdClienteAndFechaInicioLessThanEqualAndFechaVencimientoGreaterThanEqual(
            Long idCliente,
            LocalDate fechaInicio,
            LocalDate fechaVencimiento
    );
}
