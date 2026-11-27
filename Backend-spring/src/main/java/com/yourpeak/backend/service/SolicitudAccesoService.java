package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.SolicitudAccesoResponse;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.SolicitudAcceso;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.SolicitudAccesoRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Permite al cliente ver su solicitud de acceso y volver a pedirla tras un rechazo. */
@Service
public class SolicitudAccesoService {

    /** Solicitudes permitidas por día (incluye la del registro) para evitar abusos. */
    public static final int LIMITE_DIARIO = 3;
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final ClienteRepository clienteRepository;
    private final SolicitudAccesoRepository solicitudRepository;
    private final TipoEstadoRepository tipoEstadoRepository;

    public SolicitudAccesoService(ClienteRepository clienteRepository,
                                  SolicitudAccesoRepository solicitudRepository,
                                  TipoEstadoRepository tipoEstadoRepository) {
        this.clienteRepository = clienteRepository;
        this.solicitudRepository = solicitudRepository;
        this.tipoEstadoRepository = tipoEstadoRepository;
    }

    @Transactional(readOnly = true)
    public SolicitudAccesoResponse consultar(Long idUsuario) {
        Cliente cliente = clienteRepository.findByUsuario_IdUsuario(idUsuario)
                .orElseThrow(this::sinCliente);
        return respuesta(cliente);
    }

    @Transactional
    public SolicitudAccesoResponse volverASolicitar(Long idUsuario, String mensaje) {
        Cliente cliente = clienteRepository.findBloqueadoByUsuario_IdUsuario(idUsuario)
                .orElseThrow(this::sinCliente);

        if (!"RECHAZADO".equalsIgnoreCase(cliente.getTipoEstado().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo puedes volver a solicitar acceso cuando tu solicitud fue rechazada.");
        }
        if (restantesHoy(cliente) <= 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Alcanzaste el máximo de " + LIMITE_DIARIO + " solicitudes por día. Inténtalo de nuevo mañana.");
        }

        TipoEstado pendiente = tipoEstadoRepository.findByNombreIgnoreCase("PENDIENTE")
                .orElseThrow(() -> new IllegalStateException("Falta configurar el estado PENDIENTE"));
        cliente.volverAPendiente(pendiente);
        String texto = mensaje == null || mensaje.isBlank() ? null : mensaje.strip();
        solicitudRepository.save(new SolicitudAcceso(cliente, texto));
        return respuesta(cliente);
    }

    /** Guarda la primera solicitud, creada junto con la cuenta. */
    @Transactional
    public void registrarInicial(Cliente cliente) {
        solicitudRepository.save(new SolicitudAcceso(cliente, null));
    }

    private int restantesHoy(Cliente cliente) {
        LocalDateTime inicioDia = LocalDate.now(ZONA_LIMA).atStartOfDay();
        long hoy = solicitudRepository.countByCliente_IdClienteAndFechaSolicitudGreaterThanEqual(
                cliente.getIdCliente(), inicioDia);
        return (int) Math.max(0, LIMITE_DIARIO - hoy);
    }

    private SolicitudAccesoResponse respuesta(Cliente cliente) {
        List<SolicitudAcceso> solicitudes = solicitudRepository
                .findAllByCliente_IdClienteOrderByFechaSolicitudDescIdSolicitudAccesoDesc(cliente.getIdCliente());
        List<SolicitudAccesoResponse.Item> historial = new ArrayList<>();
        for (int i = 0; i < solicitudes.size(); i++) {
            SolicitudAcceso s = solicitudes.get(i);
            historial.add(new SolicitudAccesoResponse.Item(solicitudes.size() - i, s.getEstado(),
                    s.getMensajeCliente(), s.getMotivoRechazo(), s.getFechaSolicitud(), s.getFechaResolucion()));
        }
        String estado = cliente.getTipoEstado().getNombre().toUpperCase();
        int restantes = restantesHoy(cliente);
        return new SolicitudAccesoResponse(estado, "RECHAZADO".equals(estado) && restantes > 0,
                restantes, LIMITE_DIARIO, historial);
    }

    private ResponseStatusException sinCliente() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Tu cuenta no tiene un perfil de cliente.");
    }
}
