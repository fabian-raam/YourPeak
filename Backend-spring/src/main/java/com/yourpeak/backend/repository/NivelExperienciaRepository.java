package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.NivelExperiencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NivelExperienciaRepository extends JpaRepository<NivelExperiencia, Integer> {
    List<NivelExperiencia> findAllByOrderByIdNivelExperienciaAsc();
    Optional<NivelExperiencia> findByNombreIgnoreCase(String nombre);
}
