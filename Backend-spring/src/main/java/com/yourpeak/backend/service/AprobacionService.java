package com.yourpeak.backend.service;

import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.dto.ClientePendienteResponse;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class AprobacionService {

    private final AdministradorRepository administradorRepository;
    private final ClienteRepository clienteRepository;
    private final TipoEstadoRepository tipoEstadoRepository;

    public AprobacionService(
            AdministradorRepository administradorRepository,
            ClienteRepository clienteRepository,
            TipoEstadoRepository tipoEstadoRepository
    ) {
        this.administradorRepository = administradorRepository;
        this.clienteRepository = clienteRepository;
        this.tipoEstadoRepository = tipoEstadoRepository;
    }

    /**
     * Devuelve los clientes que esperan la revisión de un administrador.
     *
     * @param idUsuarioAdministrador usuario que solicita la lista
     * @return datos básicos y de registro de cada cliente pendiente
     */
    @Transactional(readOnly = true)
    public List<ClientePendienteResponse> listarPendientes(Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);

        return clienteRepository
                .findAllByTipoEstado_NombreIgnoreCaseOrderByFechaAltaAsc("PENDIENTE")
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    /**
     * Devuelve los datos completos de un cliente pendiente para su revisión.
     *
     * @param idCliente identificador del cliente a revisar
     * @param idUsuarioAdministrador usuario que solicita el detalle
     */
    @Transactional(readOnly = true)
    public ClientePendienteResponse obtenerPendiente(
            Long idCliente,
            Long idUsuarioAdministrador
    ) {
        verificarAdministrador(idUsuarioAdministrador);

        Cliente cliente = clienteRepository
                .findByIdClienteAndTipoEstado_NombreIgnoreCase(idCliente, "PENDIENTE")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un cliente pendiente con ese identificador."
                ));

        verificarCuentaCliente(cliente);
        return convertirRespuesta(cliente);
    }

    /**
     * Aprueba un cliente pendiente y persiste su nuevo estado en MySQL.
     *
     * @param idCliente identificador del cliente que se va a aprobar
     * @param idUsuarioAdministrador usuario cuya identidad se verificó en Django
     */
    @Transactional
    public void aprobarCliente(Long idCliente, Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);

        // Se carga el cliente como entidad administrada por JPA.
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el cliente indicado."
                ));

        verificarCuentaCliente(cliente);

        // Una cuenta activa o inactiva no puede aprobarse de nuevo.
        if (!"PENDIENTE".equalsIgnoreCase(cliente.getTipoEstado().getNombre())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El cliente no está pendiente de aprobación."
            );
        }

        // El estado ACTIVO debe existir previamente en el catálogo de estados.
        TipoEstado estadoActivo = tipoEstadoRepository.findByNombre("ACTIVO")
                .orElseThrow(() -> new IllegalStateException(
                        "Falta configurar el estado ACTIVO"
                ));

        // La entidad valida que la transición permitida sea PENDIENTE -> ACTIVO.
        cliente.activar(estadoActivo);

        // JPA detecta el cambio y lo escribe al confirmar esta transacción.
    }

    /** Una cuenta administrativa no participa en la aprobación de clientes. */
    private void verificarCuentaCliente(Cliente cliente) {
        if (administradorRepository.existsByUsuario_IdUsuario(cliente.getUsuario().getIdUsuario())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Las cuentas administradoras no requieren aprobación como clientes."
            );
        }
    }

    /** Comprueba que el usuario que realiza la operación esté registrado como administrador. */
    private void verificarAdministrador(Long idUsuarioAdministrador) {
        if (!administradorRepository.existsByUsuario_IdUsuario(idUsuarioAdministrador)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede realizar esta operación."
            );
        }
    }

    /** Convierte las entidades JPA a una respuesta que nunca incluye la contraseña hash. */
    private ClientePendienteResponse convertirRespuesta(Cliente cliente) {
        return new ClientePendienteResponse(
                cliente.getIdCliente(),
                cliente.getUsuario().getIdUsuario(),
                cliente.getUsuario().getNombre(),
                cliente.getUsuario().getApellido(),
                cliente.getUsuario().getCorreo(),
                cliente.getUsuario().getFechaNacimiento(),
                cliente.getFechaAlta(),
                cliente.getTipoEstado().getNombre()
        );
    }
}
