package com.yourpeak.backend.service;

import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.Membresia;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.dto.ClientePendienteResponse;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.SolicitudAccesoRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import com.yourpeak.backend.model.SolicitudAcceso;
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
    private final MembresiaService membresiaService;
    private final SolicitudAccesoRepository solicitudRepository;

    public AprobacionService(
            AdministradorRepository administradorRepository,
            ClienteRepository clienteRepository,
            TipoEstadoRepository tipoEstadoRepository,
            MembresiaService membresiaService,
            SolicitudAccesoRepository solicitudRepository
    ) {
        this.administradorRepository = administradorRepository;
        this.clienteRepository = clienteRepository;
        this.tipoEstadoRepository = tipoEstadoRepository;
        this.membresiaService = membresiaService;
        this.solicitudRepository = solicitudRepository;
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
    public Membresia aprobarCliente(
            Long idCliente,
            Integer idPlanMembresia,
            String observacion,
            Long idUsuarioAdministrador
    ) {
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
        solicitudPendiente(cliente).aprobar(idUsuarioAdministrador);
        return membresiaService.registrarPeriodo(
                idCliente,
                idPlanMembresia,
                idUsuarioAdministrador,
                observacion
        );
    }

    /**
     * Rechaza a un cliente pendiente; el cliente verá el motivo y podrá volver a solicitar acceso.
     */
    @Transactional
    public void rechazarCliente(Long idCliente, String motivo, Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);

        Cliente cliente = clienteRepository.findBloqueadoByIdCliente(idCliente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el cliente indicado."
                ));
        verificarCuentaCliente(cliente);

        if (!"PENDIENTE".equalsIgnoreCase(cliente.getTipoEstado().getNombre())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El cliente no está pendiente de aprobación."
            );
        }

        TipoEstado rechazado = tipoEstadoRepository.findByNombreIgnoreCase("RECHAZADO")
                .orElseThrow(() -> new IllegalStateException("Falta configurar el estado RECHAZADO"));
        cliente.rechazar(rechazado);
        solicitudPendiente(cliente).rechazar(motivo.strip(), idUsuarioAdministrador);
    }

    /** Devuelve la solicitud abierta; las cuentas creadas antes del historial reciben una al resolverse. */
    private SolicitudAcceso solicitudPendiente(Cliente cliente) {
        return solicitudRepository
                .findFirstByCliente_IdClienteOrderByFechaSolicitudDescIdSolicitudAccesoDesc(cliente.getIdCliente())
                .filter(solicitud -> SolicitudAcceso.PENDIENTE.equals(solicitud.getEstado()))
                .orElseGet(() -> solicitudRepository.save(new SolicitudAcceso(cliente, null)));
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
        List<SolicitudAcceso> solicitudes = solicitudRepository
                .findAllByCliente_IdClienteOrderByFechaSolicitudDescIdSolicitudAccesoDesc(cliente.getIdCliente());
        SolicitudAcceso actual = solicitudes.isEmpty() ? null : solicitudes.get(0);
        String motivoAnterior = solicitudes.stream()
                .filter(solicitud -> SolicitudAcceso.RECHAZADA.equals(solicitud.getEstado()))
                .map(SolicitudAcceso::getMotivoRechazo)
                .findFirst().orElse(null);
        return new ClientePendienteResponse(
                cliente.getIdCliente(),
                cliente.getUsuario().getIdUsuario(),
                cliente.getUsuario().getNombre(),
                cliente.getUsuario().getApellido(),
                cliente.getUsuario().getCorreo(),
                cliente.getUsuario().getFechaNacimiento(),
                cliente.getFechaAlta(),
                cliente.getTipoEstado().getNombre(),
                Math.max(1, solicitudes.size()),
                actual != null ? actual.getFechaSolicitud() : cliente.getFechaAlta(),
                actual != null ? actual.getMensajeCliente() : null,
                motivoAnterior
        );
    }
}
