package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.EjercicioRealizadoResponse;
import com.yourpeak.backend.dto.RegistrarEjercicioRealizadoRequest;
import com.yourpeak.backend.dto.RegistrarEjercicioRealizadoRequest.Serie;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.PlanEntrenamientoEjercicio;
import com.yourpeak.backend.model.SerieRealizada;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.PlanEntrenamientoEjercicioRepository;
import com.yourpeak.backend.repository.SerieRealizadaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Registra los ejercicios que un cliente activo completó en la rutina de hoy. */
@Service
public class EjecucionRutinaService {
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final ClienteRepository clienteRepository;
    private final AdministradorRepository administradorRepository;
    private final PlanEntrenamientoEjercicioRepository planEjercicioRepository;
    private final SerieRealizadaRepository serieRepository;

    public EjecucionRutinaService(ClienteRepository clienteRepository,
                                  AdministradorRepository administradorRepository,
                                  PlanEntrenamientoEjercicioRepository planEjercicioRepository,
                                  SerieRealizadaRepository serieRepository) {
        this.clienteRepository = clienteRepository;
        this.administradorRepository = administradorRepository;
        this.planEjercicioRepository = planEjercicioRepository;
        this.serieRepository = serieRepository;
    }

    /** Guarda los resultados de todas las series de un ejercicio, una sola vez por día. */
    @Transactional
    public EjercicioRealizadoResponse registrar(Long idUsuario, Long idPlanEjercicio,
                                                RegistrarEjercicioRealizadoRequest request) {
        Cliente cliente = buscarClienteActivo(idUsuario);
        PlanEntrenamientoEjercicio planEjercicio = planEjercicioRepository.findById(idPlanEjercicio)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró ese ejercicio en una rutina."));

        var plan = planEjercicio.getPlanDia().getPlanEntrenamiento();
        if (!plan.getCliente().getIdCliente().equals(cliente.getIdCliente())
                || !"ACTIVO".equalsIgnoreCase(plan.getTipoEstado().getNombre())) {
            throw accesoDenegado();
        }

        LocalDate hoy = LocalDate.now(ZONA_LIMA);
        if (planEjercicio.getPlanDia().getDiaSemana() != hoy.getDayOfWeek().getValue()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este ejercicio no pertenece a la rutina programada para hoy.");
        }
        if (!"ACTIVO".equalsIgnoreCase(planEjercicio.getEjercicio().getTipoEstado().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El ejercicio ya no está activo en el catálogo.");
        }

        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.plusDays(1).atStartOfDay();
        if (!serieRepository.findAllByPlanEjercicio_IdPlanEjercicioAndFechaHoraGreaterThanEqualAndFechaHoraLessThan(
                idPlanEjercicio, inicio, fin).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este ejercicio ya fue registrado como completado hoy.");
        }

        List<Serie> series = request.series();
        if (series.size() != planEjercicio.getSeries()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registra exactamente las " + planEjercicio.getSeries() + " series programadas.");
        }

        boolean esTiempo = "TIEMPO".equalsIgnoreCase(
                planEjercicio.getEjercicio().getTipoMedicion().getNombre());
        Set<Integer> numeros = new HashSet<>();
        for (Serie serie : series) {
            if (!numeros.add(serie.numeroSerie()) || serie.numeroSerie() > planEjercicio.getSeries()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada serie debe tener un número único entre 1 y " + planEjercicio.getSeries() + ".");
            }
            if (esTiempo ? serie.segundos() == null || serie.repeticiones() != null
                    : serie.repeticiones() == null || serie.segundos() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        esTiempo ? "Indica los segundos de cada serie." : "Indica las repeticiones de cada serie.");
            }
        }
        for (int numero = 1; numero <= planEjercicio.getSeries(); numero++) {
            if (!numeros.contains(numero)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Debes registrar todas las series, desde la número 1.");
            }
        }

        serieRepository.saveAll(series.stream().map(serie -> new SerieRealizada(
                planEjercicio, serie.numeroSerie(), serie.repeticiones(), serie.segundos()
        )).toList());

        return new EjercicioRealizadoResponse(idPlanEjercicio,
                planEjercicio.getEjercicio().getNombre(), hoy, series.size(), true,
                "Ejercicio marcado como completado.");
    }

    private Cliente buscarClienteActivo(Long idUsuario) {
        if (idUsuario == null || administradorRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw accesoDenegado();
        }
        Cliente cliente = clienteRepository.findByUsuario_IdUsuario(idUsuario)
                .orElseThrow(EjecucionRutinaService::accesoDenegado);
        if (!"ACTIVO".equalsIgnoreCase(cliente.getTipoEstado().getNombre())) throw accesoDenegado();
        return cliente;
    }

    private static ResponseStatusException accesoDenegado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo un cliente activo puede registrar sus ejercicios realizados.");
    }
}
