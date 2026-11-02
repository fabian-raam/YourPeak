package com.yourpeak.backend.service;

import com.yourpeak.backend.model.Administrador;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.Membresia;
import com.yourpeak.backend.model.PlanMembresia;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.MembresiaRepository;
import com.yourpeak.backend.repository.PlanMembresiaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class MembresiaService {

    private static final ZoneId ZONA_GIMNASIO = ZoneId.of("America/Lima");

    private final AdministradorRepository administradorRepository;
    private final ClienteRepository clienteRepository;
    private final PlanMembresiaRepository planRepository;
    private final MembresiaRepository membresiaRepository;

    public MembresiaService(
            AdministradorRepository administradorRepository,
            ClienteRepository clienteRepository,
            PlanMembresiaRepository planRepository,
            MembresiaRepository membresiaRepository
    ) {
        this.administradorRepository = administradorRepository;
        this.clienteRepository = clienteRepository;
        this.planRepository = planRepository;
        this.membresiaRepository = membresiaRepository;
    }

    /**
     * Registra un período nuevo tomando su duración del plan seleccionado.
     * Si el cliente aún tiene días pagados, el nuevo período se programa después
     * del vencimiento más lejano para conservarlos.
     */
    @Transactional
    public Membresia registrarPeriodo(
            Long idCliente,
            Integer idPlan,
            Long idUsuarioAdministrador,
            String observacion
    ) {
        Administrador administrador = administradorRepository
                .findByUsuario_IdUsuario(idUsuarioAdministrador)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Solo un administrador puede registrar una membresía."
                ));

        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el cliente."
                ));

        PlanMembresia plan = planRepository.findById(idPlan)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el plan de membresía."
                ));

        if (!"ACTIVO".equalsIgnoreCase(plan.getTipoEstado().getNombre())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede asignar un plan inactivo."
            );
        }

        LocalDate hoy = LocalDate.now(ZONA_GIMNASIO);
        LocalDate fechaInicio = membresiaRepository
                .findFirstByCliente_IdClienteOrderByFechaVencimientoDesc(idCliente)
                .filter(periodo -> !periodo.getFechaVencimiento().isBefore(hoy))
                .map(periodo -> periodo.getFechaVencimiento().plusDays(1))
                .orElse(hoy);

        // El último día del período es inclusivo: el siguiente empieza al día siguiente.
        LocalDate fechaVencimiento = fechaInicio
                .plusMonths(plan.getDuracionMeses().longValue())
                .minusDays(1);

        Membresia periodo = new Membresia(
                cliente,
                administrador,
                plan,
                fechaInicio,
                fechaVencimiento,
                observacion
        );
        return membresiaRepository.save(periodo);
    }
}
