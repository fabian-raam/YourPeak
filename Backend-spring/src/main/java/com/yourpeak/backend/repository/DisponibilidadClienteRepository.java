package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.DisponibilidadCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadClienteRepository extends JpaRepository<DisponibilidadCliente, Long> {

    List<DisponibilidadCliente> findAllByCliente_IdClienteOrderByDiaSemanaAsc(Long idCliente);

    void deleteAllByCliente_IdCliente(Long idCliente);
}
