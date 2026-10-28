package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.CambiarEstadoPlanRequest;
import com.yourpeak.backend.dto.CambiarNombrePlanRequest;
import com.yourpeak.backend.dto.PlanMembresiaRequest;
import com.yourpeak.backend.dto.PlanMembresiaResponse;
import com.yourpeak.backend.model.PlanMembresia;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.PlanMembresiaRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class PlanMembresiaService {

    private final AdministradorRepository administradorRepository;
    private final PlanMembresiaRepository planRepository;
    private final TipoEstadoRepository estadoRepository;

    public PlanMembresiaService(
            AdministradorRepository administradorRepository,
            PlanMembresiaRepository planRepository,
            TipoEstadoRepository estadoRepository
    ) {
        this.administradorRepository = administradorRepository;
        this.planRepository = planRepository;
        this.estadoRepository = estadoRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanMembresiaResponse> listar(Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        return planRepository.findAllByOrderByNombreAsc().stream()
                .map(this::respuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanMembresiaResponse obtener(Integer idPlan, Long idUsuarioAdministrador) {
        verificarAdministrador(idUsuarioAdministrador);
        return respuesta(buscarPlan(idPlan));
    }

    @Transactional
    public PlanMembresiaResponse crear(
            PlanMembresiaRequest request,
            Long idUsuarioAdministrador
    ) {
        verificarAdministrador(idUsuarioAdministrador);
        String nombre = normalizarNombre(request.nombre());
        verificarNombreDisponible(nombre, null);

        TipoEstado activo = buscarEstado("ACTIVO");
        PlanMembresia plan = new PlanMembresia(
                nombre,
                request.duracionMeses().byteValue(),
                activo
        );
        return respuesta(planRepository.save(plan));
    }

    @Transactional
    public PlanMembresiaResponse actualizar(
            Integer idPlan,
            CambiarNombrePlanRequest request,
            Long idUsuarioAdministrador
    ) {
        verificarAdministrador(idUsuarioAdministrador);
        PlanMembresia plan = buscarPlan(idPlan);
        String nombre = normalizarNombre(request.nombre());
        verificarNombreDisponible(nombre, idPlan);

        // La duración no se edita: los planes vendidos deben conservar su condición.
        plan.cambiarNombre(nombre);
        return respuesta(plan);
    }

    @Transactional
    public PlanMembresiaResponse cambiarEstado(
            Integer idPlan,
            CambiarEstadoPlanRequest request,
            Long idUsuarioAdministrador
    ) {
        verificarAdministrador(idUsuarioAdministrador);
        PlanMembresia plan = buscarPlan(idPlan);
        TipoEstado nuevoEstado = buscarEstado(request.estado());
        plan.cambiarEstado(nuevoEstado);
        return respuesta(plan);
    }

    private void verificarAdministrador(Long idUsuario) {
        if (idUsuario == null || !administradorRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede administrar planes de membresía."
            );
        }
    }

    private PlanMembresia buscarPlan(Integer idPlan) {
        return planRepository.findById(idPlan)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el plan de membresía."
                ));
    }

    private TipoEstado buscarEstado(String nombre) {
        return estadoRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta configurar el estado " + nombre + " en tipo_estado."
                ));
    }

    private void verificarNombreDisponible(String nombre, Integer idPlanActual) {
        boolean existe = idPlanActual == null
                ? planRepository.existsByNombreIgnoreCase(nombre)
                : planRepository.existsByNombreIgnoreCaseAndIdPlanMembresiaNot(nombre, idPlanActual);
        if (existe) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un plan con ese nombre."
            );
        }
    }

    private String normalizarNombre(String nombre) {
        return nombre.strip();
    }

    private PlanMembresiaResponse respuesta(PlanMembresia plan) {
        return new PlanMembresiaResponse(
                plan.getIdPlanMembresia(),
                plan.getNombre(),
                plan.getDuracionMeses().intValue(),
                plan.getTipoEstado().getNombre().toUpperCase(Locale.ROOT)
        );
    }
}
