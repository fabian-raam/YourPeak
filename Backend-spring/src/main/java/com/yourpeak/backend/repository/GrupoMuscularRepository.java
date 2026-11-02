package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.GrupoMuscular;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GrupoMuscularRepository extends JpaRepository<GrupoMuscular, Integer> {
    List<GrupoMuscular> findAllByOrderByNombreAsc();
}
