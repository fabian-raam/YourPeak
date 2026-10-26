package com.yourpeak.backend;

import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.model.Usuario;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import com.yourpeak.backend.service.AprobacionService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AprobacionServiceTests {

    private final AdministradorRepository administradores = mock(AdministradorRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final TipoEstadoRepository estados = mock(TipoEstadoRepository.class);
    private final AprobacionService servicio = new AprobacionService(
            administradores,
            clientes,
            estados
    );

    @Test
    void administradorPuedeAprobarClientePendiente() {
        Cliente cliente = clienteConEstado("PENDIENTE");
        TipoEstado activo = estado("ACTIVO");
        when(administradores.existsByUsuario_IdUsuario(7L)).thenReturn(true);
        when(clientes.findById(12L)).thenReturn(Optional.of(cliente));
        when(estados.findByNombre("ACTIVO")).thenReturn(Optional.of(activo));

        servicio.aprobarCliente(12L, 7L);

        assertEquals("ACTIVO", cliente.getTipoEstado().getNombre());
    }

    @Test
    void usuarioQueNoEsAdministradorNoPuedeAprobar() {
        when(administradores.existsByUsuario_IdUsuario(8L)).thenReturn(false);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.aprobarCliente(12L, 8L)
        );

        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(clientes, estados);
    }

    @Test
    void clienteQueYaNoEstaPendienteNoSePuedeAprobarDeNuevo() {
        Cliente cliente = clienteConEstado("ACTIVO");
        when(administradores.existsByUsuario_IdUsuario(7L)).thenReturn(true);
        when(clientes.findById(12L)).thenReturn(Optional.of(cliente));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.aprobarCliente(12L, 7L)
        );

        assertEquals(409, error.getStatusCode().value());
        verify(estados, org.mockito.Mockito.never()).findByNombre("ACTIVO");
    }

    private Cliente clienteConEstado(String nombreEstado) {
        Usuario usuario = new Usuario(
                "Ana",
                "Prueba",
                "ana@tecsup.edu.pe",
                "hash",
                LocalDate.of(2000, 1, 1)
        );
        return new Cliente(usuario, estado(nombreEstado));
    }

    @Test
    void cuentaAdministradoraNoSePuedeAprobarNiConsultarComoPendiente() {
        Cliente cliente = mock(Cliente.class);
        Usuario usuario = mock(Usuario.class);
        when(cliente.getUsuario()).thenReturn(usuario);
        when(usuario.getIdUsuario()).thenReturn(7L);
        when(administradores.existsByUsuario_IdUsuario(7L)).thenReturn(true);
        when(clientes.findById(12L)).thenReturn(Optional.of(cliente));
        when(clientes.findByIdClienteAndTipoEstado_NombreIgnoreCase(12L, "PENDIENTE"))
                .thenReturn(Optional.of(cliente));

        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> servicio.aprobarCliente(12L, 7L)).getStatusCode().value());
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> servicio.obtenerPendiente(12L, 7L)).getStatusCode().value());
        verifyNoInteractions(estados);
    }

    private TipoEstado estado(String nombre) {
        TipoEstado estado = mock(TipoEstado.class);
        when(estado.getNombre()).thenReturn(nombre);
        return estado;
    }
}
