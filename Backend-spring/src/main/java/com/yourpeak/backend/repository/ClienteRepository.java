package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.Cliente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Trae de una vez el usuario y estado relacionados para armar las respuestas.
    @EntityGraph(attributePaths = {"usuario", "tipoEstado"})
    List<Cliente> findAllByTipoEstado_NombreIgnoreCaseOrderByFechaAltaAsc(String nombreEstado);

    // El detalle solo se devuelve mientras el cliente siga pendiente.
    @EntityGraph(attributePaths = {"usuario", "tipoEstado"})
    Optional<Cliente> findByIdClienteAndTipoEstado_NombreIgnoreCase(
            Long idCliente,
            String nombreEstado
    );

    // Busca el estado del cliente asociado a una cuenta para login y perfil.
    @EntityGraph(attributePaths = {"tipoEstado"})
    Optional<Cliente> findByUsuario_IdUsuario(Long idUsuario);
}
