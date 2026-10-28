package com.yourpeak.backend;

import com.yourpeak.backend.dto.CambiarEstadoPlanRequest;
import com.yourpeak.backend.dto.CambiarNombrePlanRequest;
import com.yourpeak.backend.dto.PlanMembresiaRequest;
import com.yourpeak.backend.model.PlanMembresia;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.PlanMembresiaRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import com.yourpeak.backend.service.PlanMembresiaService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlanMembresiaServiceTests {

    private final AdministradorRepository administradores = mock(AdministradorRepository.class);
    private final PlanMembresiaRepository planes = mock(PlanMembresiaRepository.class);
    private final TipoEstadoRepository estados = mock(TipoEstadoRepository.class);
    private final PlanMembresiaService servicio = new PlanMembresiaService(
            administradores,
            planes,
            estados
    );

    @Test
    void administradorCreaPlanActivoConNombreYDuracion() {
        TipoEstado activo = estado("ACTIVO");
        when(administradores.existsByUsuario_IdUsuario(7L)).thenReturn(true);
        when(planes.existsByNombreIgnoreCase("Plan mensual")).thenReturn(false);
        when(estados.findByNombre("ACTIVO")).thenReturn(Optional.of(activo));
        when(planes.save(any(PlanMembresia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var creado = servicio.crear(new PlanMembresiaRequest("Plan mensual", 1), 7L);

        assertEquals("Plan mensual", creado.nombre());
        assertEquals(1, creado.duracionMeses());
        assertEquals("ACTIVO", creado.estado());
    }

    @Test
    void usuarioQueNoEsAdministradorNoPuedeCrearPlanes() {
        when(administradores.existsByUsuario_IdUsuario(8L)).thenReturn(false);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.crear(new PlanMembresiaRequest("Plan mensual", 1), 8L)
        );

        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(planes, estados);
    }

    @Test
    void nombreDuplicadoDevuelveConflicto() {
        when(administradores.existsByUsuario_IdUsuario(7L)).thenReturn(true);
        when(planes.existsByNombreIgnoreCase("Plan mensual")).thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.crear(new PlanMembresiaRequest("Plan mensual", 1), 7L)
        );

        assertEquals(409, error.getStatusCode().value());
        verifyNoInteractions(estados);
    }

    @Test
    void editarNombreConservaLaDuracionYElEstado() {
        TipoEstado activo = estado("ACTIVO");
        PlanMembresia plan = new PlanMembresia("Plan mensual", (byte) 1, activo);
        when(administradores.existsByUsuario_IdUsuario(7L)).thenReturn(true);
        when(planes.findById(1)).thenReturn(Optional.of(plan));
        when(planes.existsByNombreIgnoreCaseAndIdPlanMembresiaNot("Mensual renovado", 1))
                .thenReturn(false);

        var actualizado = servicio.actualizar(
                1,
                new CambiarNombrePlanRequest("Mensual renovado"),
                7L
        );

        assertEquals("Mensual renovado", actualizado.nombre());
        assertEquals(1, actualizado.duracionMeses());
        assertEquals("ACTIVO", actualizado.estado());
        verify(estados, org.mockito.Mockito.never()).findByNombre("ACTIVO");
    }

    @Test
    void administradorPuedeDesactivarYReactivarUnPlan() {
        TipoEstado activo = estado("ACTIVO");
        TipoEstado inactivo = estado("INACTIVO");
        PlanMembresia plan = new PlanMembresia("Plan mensual", (byte) 1, activo);
        when(administradores.existsByUsuario_IdUsuario(7L)).thenReturn(true);
        when(planes.findById(1)).thenReturn(Optional.of(plan));
        when(estados.findByNombre("INACTIVO")).thenReturn(Optional.of(inactivo));
        when(estados.findByNombre("ACTIVO")).thenReturn(Optional.of(activo));

        var desactivado = servicio.cambiarEstado(1, new CambiarEstadoPlanRequest("INACTIVO"), 7L);
        var reactivado = servicio.cambiarEstado(1, new CambiarEstadoPlanRequest("ACTIVO"), 7L);

        assertEquals("INACTIVO", desactivado.estado());
        assertEquals("ACTIVO", reactivado.estado());
    }

    private TipoEstado estado(String nombre) {
        TipoEstado estado = mock(TipoEstado.class);
        when(estado.getNombre()).thenReturn(nombre);
        return estado;
    }
}
