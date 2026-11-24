package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.SerieRealizada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SerieRealizadaRepository extends JpaRepository<SerieRealizada, Long> {
    List<SerieRealizada> findAllByPlanEjercicio_IdPlanEjercicioAndFechaHoraGreaterThanEqualAndFechaHoraLessThan(
            Long idPlanEjercicio, LocalDateTime inicio, LocalDateTime fin
    );
}
