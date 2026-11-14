package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.NivelActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NivelActividadRepository extends JpaRepository<NivelActividad, Integer> {
    List<NivelActividad> findAllByOrderByIdNivelActividadAsc();
}
