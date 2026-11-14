package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.TipoObjetivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TipoObjetivoRepository extends JpaRepository<TipoObjetivo, Integer> {
    List<TipoObjetivo> findAllByOrderByIdTipoObjetivoAsc();
}
