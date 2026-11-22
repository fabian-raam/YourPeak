package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.OpcionPerfilResponse;
import com.yourpeak.backend.dto.PerfilCatalogosResponse;
import com.yourpeak.backend.dto.PerfilInicialRequest;
import com.yourpeak.backend.dto.PerfilInicialResponse;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.RegistroFisico;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.NivelActividadRepository;
import com.yourpeak.backend.repository.NivelExperienciaRepository;
import com.yourpeak.backend.repository.RegistroFisicoRepository;
import com.yourpeak.backend.repository.TipoObjetivoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;

/** Guarda y consulta el perfil inicial de un cliente, incluidas sus medidas físicas. */
@Service
public class PerfilClienteService {
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final ClienteRepository clienteRepository;
    private final AdministradorRepository administradorRepository;
    private final TipoObjetivoRepository objetivoRepository;
    private final NivelExperienciaRepository experienciaRepository;
    private final NivelActividadRepository actividadRepository;
    private final RegistroFisicoRepository registroFisicoRepository;
    private final MetaNutricionalService metaNutricionalService;

    public PerfilClienteService(
            ClienteRepository clienteRepository,
            AdministradorRepository administradorRepository,
            TipoObjetivoRepository objetivoRepository,
            NivelExperienciaRepository experienciaRepository,
            NivelActividadRepository actividadRepository,
            RegistroFisicoRepository registroFisicoRepository,
            MetaNutricionalService metaNutricionalService
    ) {
        this.clienteRepository = clienteRepository;
        this.administradorRepository = administradorRepository;
        this.objetivoRepository = objetivoRepository;
        this.experienciaRepository = experienciaRepository;
        this.actividadRepository = actividadRepository;
        this.registroFisicoRepository = registroFisicoRepository;
        this.metaNutricionalService = metaNutricionalService;
    }

    @Transactional(readOnly = true)
    public PerfilInicialResponse consultar(Long idUsuario) {
        Cliente cliente = buscarClienteActivo(idUsuario);
        var registro = registroFisicoRepository
                .findFirstByCliente_IdClienteOrderByFechaRegistroDesc(cliente.getIdCliente());
        var objetivo = cliente.getIdTipoObjetivo() == null ? null
                : objetivoRepository.findById(cliente.getIdTipoObjetivo()).orElse(null);
        var experiencia = cliente.getIdNivelExperiencia() == null ? null
                : experienciaRepository.findById(cliente.getIdNivelExperiencia()).orElse(null);
        var actividad = cliente.getIdNivelActividad() == null ? null
                : actividadRepository.findById(cliente.getIdNivelActividad()).orElse(null);

        boolean completado = objetivo != null && experiencia != null && actividad != null
                && cliente.getSexo() != null
                && registro.isPresent();
        return new PerfilInicialResponse(
                completado,
                cliente.getIdTipoObjetivo(), objetivo == null ? null : objetivo.getNombre(),
                cliente.getIdNivelExperiencia(), experiencia == null ? null : experiencia.getNombre(),
                cliente.getIdNivelActividad(), actividad == null ? null : actividad.getNombre(),
                cliente.getSexo(),
                registro.map(RegistroFisico::getPesoKg).orElse(null),
                registro.map(RegistroFisico::getTallaCm).orElse(null),
                registro.map(RegistroFisico::getFechaRegistro).orElse(null),
                cargarCatalogos()
        );
    }

    @Transactional
    public PerfilInicialResponse guardar(Long idUsuario, PerfilInicialRequest request) {
        Cliente cliente = buscarClienteActivo(idUsuario);
        if (cliente.getSexo() != null && !cliente.getSexo().equals(request.sexo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El sexo solo se puede registrar una vez y no puede modificarse.");
        }
        var objetivo = objetivoRepository.findById(request.idTipoObjetivo())
                .orElseThrow(() -> datoCatalogoInvalido("objetivo"));
        var experiencia = experienciaRepository.findById(request.idNivelExperiencia())
                .orElseThrow(() -> datoCatalogoInvalido("nivel de experiencia"));
        var actividad = actividadRepository.findById(request.idNivelActividad())
                .orElseThrow(() -> datoCatalogoInvalido("nivel de actividad"));

        cliente.actualizarPerfil(
                objetivo.getIdTipoObjetivo(),
                experiencia.getIdNivelExperiencia(),
                actividad.getIdNivelActividad(),
                request.sexo()
        );

        var ultimaMedicion = registroFisicoRepository
                .findFirstByCliente_IdClienteOrderByFechaRegistroDesc(cliente.getIdCliente());
        RegistroFisico medicion = ultimaMedicion.orElse(null);
        boolean cambioMedidas = ultimaMedicion.isEmpty()
                || ultimaMedicion.get().getPesoKg().compareTo(request.pesoKg()) != 0
                || ultimaMedicion.get().getTallaCm().compareTo(request.tallaCm()) != 0;
        if (cambioMedidas) {
            medicion = registroFisicoRepository.save(new RegistroFisico(
                    cliente, request.pesoKg(), request.tallaCm()));
        }

        metaNutricionalService.actualizar(cliente, medicion, LocalDate.now(ZONA_LIMA));

        return consultar(idUsuario);
    }

    private PerfilCatalogosResponse cargarCatalogos() {
        List<OpcionPerfilResponse> objetivos = objetivoRepository
                .findAllByOrderByIdTipoObjetivoAsc().stream()
                .map(item -> new OpcionPerfilResponse(
                        item.getIdTipoObjetivo(), item.getNombre(), item.getDescripcion()
                )).toList();
        List<OpcionPerfilResponse> experiencias = experienciaRepository
                .findAllByOrderByIdNivelExperienciaAsc().stream()
                .map(item -> new OpcionPerfilResponse(
                        item.getIdNivelExperiencia(), item.getNombre(), item.getDescripcion()
                )).toList();
        List<OpcionPerfilResponse> actividades = actividadRepository
                .findAllByOrderByIdNivelActividadAsc().stream()
                .map(item -> new OpcionPerfilResponse(
                        item.getIdNivelActividad(), item.getNombre(), item.getDescripcion()
                )).toList();
        return new PerfilCatalogosResponse(objetivos, experiencias, actividades);
    }

    /** Nunca toma un idCliente del navegador: resuelve el perfil desde el usuario validado. */
    private Cliente buscarClienteActivo(Long idUsuario) {
        if (idUsuario == null || administradorRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw accesoDenegado();
        }
        Cliente cliente = clienteRepository.findByUsuario_IdUsuario(idUsuario)
                .orElseThrow(PerfilClienteService::accesoDenegado);
        if (!"ACTIVO".equalsIgnoreCase(cliente.getTipoEstado().getNombre())) {
            throw accesoDenegado();
        }
        return cliente;
    }

    private static ResponseStatusException datoCatalogoInvalido(String campo) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El " + campo + " seleccionado no existe.");
    }

    private static ResponseStatusException accesoDenegado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo una cuenta cliente activa puede gestionar su perfil.");
    }
}
