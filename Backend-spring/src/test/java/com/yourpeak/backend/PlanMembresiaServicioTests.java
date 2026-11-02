package com.yourpeak.backend;

import com.yourpeak.backend.dto.PlanMembresiaRequest;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.PlanMembresiaRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import com.yourpeak.backend.service.PlanMembresiaService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlanMembresiaServicioTests {

    private final AdministradorRepository administradores = mock(AdministradorRepository.class);
    private final PlanMembresiaRepository planes = mock(PlanMembresiaRepository.class);
    private final TipoEstadoRepository estados = mock(TipoEstadoRepository.class);
    private final PlanMembresiaService servicio = new PlanMembresiaService(
            administradores,
            planes,
            estados
    );

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

}
