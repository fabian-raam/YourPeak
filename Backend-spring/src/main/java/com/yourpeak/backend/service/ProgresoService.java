package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.RegistroProgresoRequest;
import com.yourpeak.backend.dto.RegistroProgresoResponse;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.RegistroFisico;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.RegistroFisicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;

/** Consulta el historial de peso y registra mediciones nuevas del cliente activo. */
@Service
public class ProgresoService {
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final ClienteRepository clienteRepository;
    private final AdministradorRepository administradorRepository;
    private final RegistroFisicoRepository registroFisicoRepository;
    private final MetaNutricionalService metaNutricionalService;

    public ProgresoService(ClienteRepository clienteRepository,
                           AdministradorRepository administradorRepository,
                           RegistroFisicoRepository registroFisicoRepository,
                           MetaNutricionalService metaNutricionalService) {
        this.clienteRepository = clienteRepository;
        this.administradorRepository = administradorRepository;
        this.registroFisicoRepository = registroFisicoRepository;
        this.metaNutricionalService = metaNutricionalService;
    }

    @Transactional(readOnly = true)
    public List<RegistroProgresoResponse> consultarHistorial(Long idUsuario) {
        Cliente cliente = buscarClienteActivo(idUsuario);
        return registroFisicoRepository
                .findAllByCliente_IdClienteOrderByFechaRegistroDesc(cliente.getIdCliente())
                .stream().map(this::respuesta).toList();
    }

    @Transactional
    public RegistroProgresoResponse registrarPeso(Long idUsuario, RegistroProgresoRequest request) {
        Cliente cliente = buscarClienteActivo(idUsuario);
        RegistroFisico ultimaMedicion = registroFisicoRepository
                .findFirstByCliente_IdClienteOrderByFechaRegistroDesc(cliente.getIdCliente())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Completa primero tu perfil para registrar tu peso."));

        RegistroFisico nuevaMedicion = registroFisicoRepository.save(
                new RegistroFisico(cliente, request.pesoKg(), ultimaMedicion.getTallaCm()));
        metaNutricionalService.actualizar(cliente, nuevaMedicion, LocalDate.now(ZONA_LIMA));
        return respuesta(nuevaMedicion);
    }

    private Cliente buscarClienteActivo(Long idUsuario) {
        if (idUsuario == null || administradorRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw accesoDenegado();
        }
        Cliente cliente = clienteRepository.findByUsuario_IdUsuario(idUsuario)
                .orElseThrow(ProgresoService::accesoDenegado);
        if (!"ACTIVO".equalsIgnoreCase(cliente.getTipoEstado().getNombre())) {
            throw accesoDenegado();
        }
        return cliente;
    }

    private RegistroProgresoResponse respuesta(RegistroFisico registro) {
        return new RegistroProgresoResponse(registro.getIdRegistroFisico(), registro.getPesoKg(),
                registro.getTallaCm(), registro.getFechaRegistro());
    }

    private static ResponseStatusException accesoDenegado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo una cuenta cliente activa puede consultar o registrar su progreso.");
    }
}
