package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.TipoMedicion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TipoMedicionRepository extends JpaRepository<TipoMedicion, Short> {
    List<TipoMedicion> findAllByOrderByIdTipoMedicionAsc();
}
