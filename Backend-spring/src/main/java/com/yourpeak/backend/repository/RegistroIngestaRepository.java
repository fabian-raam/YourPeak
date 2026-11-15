package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.RegistroIngesta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RegistroIngestaRepository extends JpaRepository<RegistroIngesta, Long> {
    @EntityGraph(attributePaths = "alimento")
    List<RegistroIngesta> findAllByCliente_IdClienteAndFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(
            Long idCliente, LocalDateTime inicio, LocalDateTime fin);

    @EntityGraph(attributePaths = "alimento")
    Optional<RegistroIngesta> findByIdIngestaAndCliente_IdCliente(Long idIngesta, Long idCliente);

    boolean existsByAlimento_IdAlimento(Integer idAlimento);
}
