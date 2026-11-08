package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.DiaRutinaRequest;
import com.yourpeak.backend.dto.DiaRutinaResponse;
import com.yourpeak.backend.dto.EjercicioRutinaRequest;
import com.yourpeak.backend.dto.EjercicioRutinaResponse;
import com.yourpeak.backend.dto.EjercicioResponse;
import com.yourpeak.backend.dto.RutinaRequest;
import com.yourpeak.backend.dto.RutinaResponse;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.DisponibilidadCliente;
import com.yourpeak.backend.model.Ejercicio;
import com.yourpeak.backend.model.NivelExperiencia;
import com.yourpeak.backend.model.PlanEntrenamiento;
import com.yourpeak.backend.model.PlanEntrenamientoDia;
import com.yourpeak.backend.model.PlanEntrenamientoEjercicio;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.model.TipoOrigenEjercicio;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.DisponibilidadClienteRepository;
import com.yourpeak.backend.repository.EjercicioRepository;
import com.yourpeak.backend.repository.NivelExperienciaRepository;
import com.yourpeak.backend.repository.PlanEntrenamientoDiaRepository;
import com.yourpeak.backend.repository.PlanEntrenamientoEjercicioRepository;
import com.yourpeak.backend.repository.PlanEntrenamientoRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import com.yourpeak.backend.repository.TipoOrigenEjercicioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lee y mantiene una versión semanal editable de la rutina del cliente activo. */
@Service
public class RutinaService {

    private final ClienteRepository clienteRepository;
    private final AdministradorRepository administradorRepository;
    private final DisponibilidadClienteRepository disponibilidadRepository;
    private final EjercicioRepository ejercicioRepository;
    private final NivelExperienciaRepository nivelRepository;
    private final PlanEntrenamientoRepository planRepository;
    private final PlanEntrenamientoDiaRepository diaRepository;
    private final PlanEntrenamientoEjercicioRepository planEjercicioRepository;
    private final TipoEstadoRepository estadoRepository;
    private final TipoOrigenEjercicioRepository origenRepository;

    public RutinaService(ClienteRepository clienteRepository,
                         AdministradorRepository administradorRepository,
                         DisponibilidadClienteRepository disponibilidadRepository,
                         EjercicioRepository ejercicioRepository,
                         NivelExperienciaRepository nivelRepository,
                         PlanEntrenamientoRepository planRepository,
                         PlanEntrenamientoDiaRepository diaRepository,
                         PlanEntrenamientoEjercicioRepository planEjercicioRepository,
                         TipoEstadoRepository estadoRepository,
                         TipoOrigenEjercicioRepository origenRepository) {
        this.clienteRepository = clienteRepository;
        this.administradorRepository = administradorRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.ejercicioRepository = ejercicioRepository;
        this.nivelRepository = nivelRepository;
        this.planRepository = planRepository;
        this.diaRepository = diaRepository;
        this.planEjercicioRepository = planEjercicioRepository;
        this.estadoRepository = estadoRepository;
        this.origenRepository = origenRepository;
    }

    /** Devuelve los siete días, aunque aún no haya rutina o disponibilidad guardada. */
    @Transactional(readOnly = true)
    public RutinaResponse consultar(Long idUsuario) {
        Cliente cliente = buscarClienteActivo(idUsuario);
        Map<Integer, DisponibilidadCliente> disponibilidad = new HashMap<>();
        disponibilidadRepository.findAllByCliente_IdClienteOrderByDiaSemanaAsc(cliente.getIdCliente())
                .forEach(dia -> disponibilidad.put(dia.getDiaSemana(), dia));

        Map<Integer, PlanEntrenamientoDia> diasRutina = new HashMap<>();
        planRepository.findFirstByCliente_IdClienteAndTipoEstado_NombreIgnoreCaseOrderByFechaGeneracionDesc(
                cliente.getIdCliente(), "ACTIVO"
        ).ifPresent(plan -> diaRepository
                .findAllByPlanEntrenamiento_IdPlanEntrenamientoOrderByDiaSemanaAsc(plan.getIdPlanEntrenamiento())
                .forEach(dia -> diasRutina.put(dia.getDiaSemana(), dia)));

        List<DiaRutinaResponse> dias = new ArrayList<>(7);
        for (int numeroDia = 1; numeroDia <= 7; numeroDia++) {
            DisponibilidadCliente horario = disponibilidad.get(numeroDia);
            boolean disponible = horario != null;
            PlanEntrenamientoDia diaRutina = diasRutina.get(numeroDia);
            List<EjercicioRutinaResponse> ejercicios = disponible && diaRutina != null
                    ? planEjercicioRepository.findAllByPlanDia_IdPlanDiaOrderByOrdenAsc(diaRutina.getIdPlanDia())
                    .stream().map(this::respuestaEjercicio).toList()
                    : List.of();

            dias.add(new DiaRutinaResponse(
                    numeroDia,
                    disponible,
                    disponible ? horario.getHoraInicio() : null,
                    disponible ? horario.getHoraFin() : null,
                    ejercicios
            ));
        }
        return new RutinaResponse(dias);
    }

    /** Catálogo de ejercicios activos que el cliente puede seleccionar para su rutina. */
    @Transactional(readOnly = true)
    public List<EjercicioResponse> listarEjerciciosDisponibles(Long idUsuario) {
        buscarClienteActivo(idUsuario);
        return ejercicioRepository.findAllByOrderByNombreAsc().stream()
                .filter(ejercicio -> "ACTIVO".equalsIgnoreCase(ejercicio.getTipoEstado().getNombre()))
                .map(ejercicio -> new EjercicioResponse(
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
                        ejercicio.getTipoEstado().getNombre()
                )).toList();
    }

    /** Reemplaza la rutina activa; los planes previos quedan inactivos para conservar su historial. */
    @Transactional
    public RutinaResponse actualizar(Long idUsuario, RutinaRequest request) {
        Cliente cliente = buscarClienteActivo(idUsuario);
        Map<Integer, DisponibilidadCliente> disponibilidad = cargarDisponibilidad(cliente);
        Map<Integer, List<Prescripcion>> prescripciones = validarRutina(request, disponibilidad);
        boolean tieneEjercicios = prescripciones.values().stream().anyMatch(dia -> !dia.isEmpty());

        TipoEstado inactivo = buscarEstado("INACTIVO");
        planRepository.findFirstByCliente_IdClienteAndTipoEstado_NombreIgnoreCaseOrderByFechaGeneracionDesc(
                cliente.getIdCliente(), "ACTIVO"
        ).ifPresent(plan -> plan.cambiarEstado(inactivo));

        // Si la persona quitó todos sus ejercicios, dejar sin plan activo también es válido.
        if (!tieneEjercicios) return consultar(idUsuario);

        NivelExperiencia nivel = resolverNivel(cliente);
        PlanEntrenamiento plan = planRepository.save(
                new PlanEntrenamiento(cliente, nivel, buscarEstado("ACTIVO"))
        );
        TipoOrigenEjercicio origenUsuario = origenRepository.findByNombreIgnoreCase("USUARIO")
                .orElseThrow(() -> new IllegalStateException(
                        "Falta configurar USUARIO en tipo_origen_ejercicio."
                ));
        TipoEstado activo = buscarEstado("ACTIVO");

        for (Map.Entry<Integer, List<Prescripcion>> entrada : prescripciones.entrySet()) {
            if (entrada.getValue().isEmpty()) continue;
            DisponibilidadCliente horario = disponibilidad.get(entrada.getKey());
            PlanEntrenamientoDia dia = diaRepository.save(new PlanEntrenamientoDia(
                    plan, entrada.getKey(), horario.getHoraInicio(), horario.getHoraFin()
            ));
            List<PlanEntrenamientoEjercicio> filas = new ArrayList<>();
            int orden = 1;
            for (Prescripcion prescripcion : entrada.getValue()) {
                EjercicioRutinaRequest datos = prescripcion.datos();
                filas.add(new PlanEntrenamientoEjercicio(
                        dia, prescripcion.ejercicio(), orden++, datos.series(), datos.repeticiones(),
                        datos.segundos(), datos.descansoSegundos(), origenUsuario, activo
                ));
            }
            planEjercicioRepository.saveAll(filas);
        }
        return consultar(idUsuario);
    }

    private Map<Integer, DisponibilidadCliente> cargarDisponibilidad(Cliente cliente) {
        Map<Integer, DisponibilidadCliente> disponibilidad = new HashMap<>();
        disponibilidadRepository.findAllByCliente_IdClienteOrderByDiaSemanaAsc(cliente.getIdCliente())
                .forEach(dia -> disponibilidad.put(dia.getDiaSemana(), dia));
        return disponibilidad;
    }

    private Map<Integer, List<Prescripcion>> validarRutina(
            RutinaRequest request, Map<Integer, DisponibilidadCliente> disponibilidad
    ) {
        if (request.dias() == null || request.dias().size() != 7) {
            throw error(HttpStatus.BAD_REQUEST, "Envía exactamente los siete días de la semana.");
        }
        Map<Integer, List<Prescripcion>> porDia = new HashMap<>();
        for (DiaRutinaRequest dia : request.dias()) {
            Integer numeroDia = dia.diaSemana();
            if (numeroDia == null || numeroDia < 1 || numeroDia > 7 || porDia.containsKey(numeroDia)) {
                throw error(HttpStatus.BAD_REQUEST, "Cada día debe aparecer una sola vez, usando valores del 1 al 7.");
            }
            if (dia.ejercicios() == null) {
                throw error(HttpStatus.BAD_REQUEST, "Envía una lista de ejercicios para cada día.");
            }
            if (!dia.ejercicios().isEmpty() && !disponibilidad.containsKey(numeroDia)) {
                throw error(HttpStatus.BAD_REQUEST, "Solo puedes programar ejercicios en días que marcaste como disponibles.");
            }
            if (dia.ejercicios().size() > 100) {
                throw error(HttpStatus.BAD_REQUEST, "Un día no puede tener más de 100 ejercicios.");
            }

            Set<Integer> idsEjercicio = new HashSet<>();
            List<Prescripcion> ejercicios = new ArrayList<>();
            for (EjercicioRutinaRequest datos : dia.ejercicios()) {
                if (datos.idEjercicio() == null || !idsEjercicio.add(datos.idEjercicio())) {
                    throw error(HttpStatus.BAD_REQUEST, "No repitas el mismo ejercicio dentro de un día.");
                }
                Ejercicio ejercicio = ejercicioRepository.findById(datos.idEjercicio())
                        .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "Uno de los ejercicios seleccionados no existe."));
                if (!"ACTIVO".equalsIgnoreCase(ejercicio.getTipoEstado().getNombre())) {
                    throw error(HttpStatus.BAD_REQUEST, "Solo puedes agregar ejercicios activos a tu rutina.");
                }
                validarMedicion(ejercicio, datos);
                ejercicios.add(new Prescripcion(ejercicio, datos));
            }
            porDia.put(numeroDia, ejercicios);
        }
        if (porDia.size() != 7) {
            throw error(HttpStatus.BAD_REQUEST, "Cada uno de los siete días debe aparecer en la solicitud.");
        }
        return porDia;
    }

    private void validarMedicion(Ejercicio ejercicio, EjercicioRutinaRequest datos) {
        String medicion = ejercicio.getTipoMedicion().getNombre();
        boolean repeticiones = "REPETICIONES".equalsIgnoreCase(medicion);
        boolean tiempo = "TIEMPO".equalsIgnoreCase(medicion);
        boolean datosValidos = repeticiones && datos.repeticiones() != null && datos.segundos() == null
                || tiempo && datos.segundos() != null && datos.repeticiones() == null;
        if (!datosValidos) {
            throw error(HttpStatus.BAD_REQUEST,
                    "El ejercicio «" + ejercicio.getNombre() + "» requiere "
                            + (repeticiones ? "repeticiones" : tiempo ? "duración en segundos" : "un tipo de medición válido")
                            + ". Envía solo el dato correspondiente.");
        }
    }

    private NivelExperiencia resolverNivel(Cliente cliente) {
        if (cliente.getIdNivelExperiencia() != null) {
            return nivelRepository.findById(cliente.getIdNivelExperiencia())
                    .orElseThrow(() -> error(HttpStatus.CONFLICT, "El nivel de experiencia del perfil no existe."));
        }
        return nivelRepository.findByNombreIgnoreCase("Principiante")
                .orElseThrow(() -> new IllegalStateException("Falta configurar el nivel Principiante."));
    }

    /** La identidad recibida por el header debe corresponder a una cuenta cliente activa. */
    private Cliente buscarClienteActivo(Long idUsuario) {
        if (idUsuario == null || administradorRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw accesoDenegado();
        }
        Cliente cliente = clienteRepository.findByUsuario_IdUsuario(idUsuario)
                .orElseThrow(RutinaService::accesoDenegado);
        if (!"ACTIVO".equalsIgnoreCase(cliente.getTipoEstado().getNombre())) throw accesoDenegado();
        return cliente;
    }

    private TipoEstado buscarEstado(String nombre) {
        return estadoRepository.findByNombre(nombre).orElseThrow(() -> new IllegalStateException(
                "Falta configurar el estado " + nombre + " en tipo_estado."
        ));
    }

    private EjercicioRutinaResponse respuestaEjercicio(PlanEntrenamientoEjercicio fila) {
        return new EjercicioRutinaResponse(
                fila.getEjercicio().getIdEjercicio(), fila.getEjercicio().getNombre(),
                fila.getEjercicio().getGrupoMuscular().getNombre(), fila.getEjercicio().getTipoMedicion().getNombre(),
                fila.getOrden(), fila.getSeries(), fila.getRepeticiones(), fila.getSegundos(),
                fila.getDescansoSegundos(), fila.getTipoOrigen().getNombre()
        );
    }

    private static ResponseStatusException accesoDenegado() {
        return error(HttpStatus.FORBIDDEN, "Solo un cliente con cuenta activa puede gestionar su rutina.");
    }

    private static ResponseStatusException error(HttpStatus status, String mensaje) {
        return new ResponseStatusException(status, mensaje);
    }

    private record Prescripcion(Ejercicio ejercicio, EjercicioRutinaRequest datos) { }
}
