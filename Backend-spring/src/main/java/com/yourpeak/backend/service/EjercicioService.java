package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.CambiarEstadoEjercicioRequest;
import com.yourpeak.backend.dto.EjercicioCatalogosResponse;
import com.yourpeak.backend.dto.EjercicioRequest;
import com.yourpeak.backend.dto.EjercicioResponse;
import com.yourpeak.backend.dto.OpcionCatalogoResponse;
import com.yourpeak.backend.model.Ejercicio;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.EjercicioRepository;
import com.yourpeak.backend.repository.GrupoMuscularRepository;
import com.yourpeak.backend.repository.NivelExperienciaRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import com.yourpeak.backend.repository.TipoMedicionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class EjercicioService {
    private final AdministradorRepository administradorRepository;
    private final EjercicioRepository ejercicioRepository;
    private final GrupoMuscularRepository grupoRepository;
    private final NivelExperienciaRepository nivelRepository;
    private final TipoMedicionRepository medicionRepository;
    private final TipoEstadoRepository estadoRepository;

    public EjercicioService(
            AdministradorRepository administradorRepository,
            EjercicioRepository ejercicioRepository,
            GrupoMuscularRepository grupoRepository,
            NivelExperienciaRepository nivelRepository,
            TipoMedicionRepository medicionRepository,
            TipoEstadoRepository estadoRepository
    ) {
        this.administradorRepository = administradorRepository;
        this.ejercicioRepository = ejercicioRepository;
        this.grupoRepository = grupoRepository;
        this.nivelRepository = nivelRepository;
        this.medicionRepository = medicionRepository;
        this.estadoRepository = estadoRepository;
    }

    @Transactional(readOnly = true)
    public List<EjercicioResponse> listar(Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        return ejercicioRepository.findAllByOrderByNombreAsc().stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public EjercicioResponse obtener(Integer idEjercicio, Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        return respuesta(buscarEjercicio(idEjercicio));
    }

    /** Expone las opciones existentes en SQL para los selectores de la interfaz. */
    @Transactional(readOnly = true)
    public EjercicioCatalogosResponse catalogos(Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        return new EjercicioCatalogosResponse(
                grupoRepository.findAllByOrderByNombreAsc().stream()
                        .map(grupo -> new OpcionCatalogoResponse(
                                grupo.getIdGrupoMuscular(), grupo.getNombre(), null)).toList(),
                nivelRepository.findAllByOrderByIdNivelExperienciaAsc().stream()
                        .map(nivel -> new OpcionCatalogoResponse(
                                nivel.getIdNivelExperiencia(), nivel.getNombre(), null)).toList(),
                medicionRepository.findAllByOrderByIdTipoMedicionAsc().stream()
                        .map(tipo -> new OpcionCatalogoResponse(
                                tipo.getIdTipoMedicion().intValue(), tipo.getNombre(), tipo.getDescripcion())).toList()
        );
    }

    @Transactional
    public EjercicioResponse crear(EjercicioRequest request, Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        String nombre = normalizarNombre(request.nombre());
        verificarNombreDisponible(nombre, null);
        Ejercicio ejercicio = new Ejercicio(
                buscarGrupo(request.idGrupoMuscular()),
                buscarNivel(request.idNivelExperiencia()),
                buscarMedicion(request.idTipoMedicion()),
                nombre,
                request.descripcion(),
                request.equipamiento(),
                request.imagenRuta(),
                buscarEstado("ACTIVO")
        );
        return respuesta(ejercicioRepository.save(ejercicio));
    }

    @Transactional
    public EjercicioResponse actualizar(
            Integer idEjercicio,
            EjercicioRequest request,
            Long idUsuarioAdministrador
    ) {
        verificarAdministrador(idUsuarioAdministrador);
        Ejercicio ejercicio = buscarEjercicio(idEjercicio);
        String nombre = normalizarNombre(request.nombre());
        verificarNombreDisponible(nombre, idEjercicio);
        ejercicio.actualizar(
                buscarGrupo(request.idGrupoMuscular()),
                buscarNivel(request.idNivelExperiencia()),
                buscarMedicion(request.idTipoMedicion()),
                nombre,
                request.descripcion(),
                request.equipamiento(),
                request.imagenRuta()
        );
        return respuesta(ejercicio);
    }

    @Transactional
    public EjercicioResponse cambiarEstado(
            Integer idEjercicio,
            CambiarEstadoEjercicioRequest request,
            Long idUsuarioAdministrador
    ) {
        verificarAdministrador(idUsuarioAdministrador);
        Ejercicio ejercicio = buscarEjercicio(idEjercicio);
        ejercicio.cambiarEstado(buscarEstado(request.estado()));
        return respuesta(ejercicio);
    }

    private void verificarAdministrador(Long idUsuario) {
        if (idUsuario == null || !administradorRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede gestionar ejercicios."
            );
        }
    }

    private Ejercicio buscarEjercicio(Integer id) {
        return ejercicioRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No se encontró el ejercicio solicitado."
        ));
    }

    private com.yourpeak.backend.model.GrupoMuscular buscarGrupo(Integer id) {
        return grupoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "El grupo muscular seleccionado no existe."
        ));
    }

    private com.yourpeak.backend.model.NivelExperiencia buscarNivel(Integer id) {
        return nivelRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "El nivel de experiencia seleccionado no existe."
        ));
    }

    private com.yourpeak.backend.model.TipoMedicion buscarMedicion(Short id) {
        return medicionRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "El tipo de medición seleccionado no existe."
        ));
    }

    private TipoEstado buscarEstado(String nombre) {
        return estadoRepository.findByNombre(nombre).orElseThrow(() -> new IllegalStateException(
                "Falta configurar el estado " + nombre + " en tipo_estado."
        ));
    }

    private void verificarNombreDisponible(String nombre, Integer idActual) {
        boolean existe = idActual == null
                ? ejercicioRepository.existsByNombreIgnoreCase(nombre)
                : ejercicioRepository.existsByNombreIgnoreCaseAndIdEjercicioNot(nombre, idActual);
        if (existe) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un ejercicio con ese nombre.");
        }
    }

    private String normalizarNombre(String nombre) { return nombre.strip(); }

    private EjercicioResponse respuesta(Ejercicio ejercicio) {
        return new EjercicioResponse(
                ejercicio.getIdEjercicio(),
                ejercicio.getGrupoMuscular().getIdGrupoMuscular(),
                ejercicio.getGrupoMuscular().getNombre(),
                ejercicio.getNivelExperiencia().getIdNivelExperiencia(),
                ejercicio.getNivelExperiencia().getNombre(),
                ejercicio.getTipoMedicion().getIdTipoMedicion(),
                ejercicio.getTipoMedicion().getNombre(),
                ejercicio.getNombre(),
                ejercicio.getDescripcion(),
                ejercicio.getEquipamiento(),
                ejercicio.getImagenRuta(),
                ejercicio.getTipoEstado().getNombre().toUpperCase(Locale.ROOT)
        );
    }
}
