package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.Membresia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    /**
     * Devuelve el período del cliente que termina más tarde.
     * Sirve para calcular una renovación sin perder días ya pagados.
     */
    Optional<Membresia> findFirstByCliente_IdClienteOrderByFechaVencimientoDesc(
            Long idCliente
    );
}
