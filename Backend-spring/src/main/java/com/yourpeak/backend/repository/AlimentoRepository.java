package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.Alimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlimentoRepository extends JpaRepository<Alimento, Integer> {
    List<Alimento> findAllByOrderByNombreAsc();
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdAlimentoNot(String nombre, Integer idAlimento);
}
