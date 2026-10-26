package com.yourpeak.backend;

import com.yourpeak.backend.dto.LoginRequest;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.model.Usuario;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.UsuarioRepository;
import com.yourpeak.backend.service.LoginService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginServiceTests {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final AdministradorRepository administradores = mock(AdministradorRepository.class);
    private final PasswordEncoder codificador = mock(PasswordEncoder.class);
    private final LoginService servicio = new LoginService(
            usuarios,
            clientes,
            administradores,
            codificador
    );

    @Test
    void rolAdministradorTienePrioridadSobrePerfilClientePendiente() {
        Usuario usuario = usuario();
        when(usuarios.findByCorreo(usuario.getCorreo())).thenReturn(Optional.of(usuario));
        when(codificador.matches("clave", "hash")).thenReturn(true);
        when(administradores.existsByUsuario_IdUsuario(5L)).thenReturn(true);

        var respuesta = servicio.autenticar(new LoginRequest(usuario.getCorreo(), "clave"))
                .orElseThrow();

        assertEquals("ADMINISTRADOR", respuesta.rol());
        assertNull(respuesta.estado());
        verifyNoInteractions(clientes);
    }

    @Test
    void clientePendienteRecibeSuRolYEstadoEnLogin() {
        Usuario usuario = usuario();
        TipoEstado pendiente = mock(TipoEstado.class);
        when(pendiente.getNombre()).thenReturn("PENDIENTE");
        when(usuarios.findByCorreo(usuario.getCorreo())).thenReturn(Optional.of(usuario));
        when(codificador.matches("clave", "hash")).thenReturn(true);
        when(administradores.existsByUsuario_IdUsuario(5L)).thenReturn(false);
        when(clientes.findByUsuario_IdUsuario(5L))
                .thenReturn(Optional.of(new Cliente(usuario, pendiente)));

        var respuesta = servicio.autenticar(new LoginRequest(usuario.getCorreo(), "clave"))
                .orElseThrow();

        assertEquals("CLIENTE", respuesta.rol());
        assertEquals("PENDIENTE", respuesta.estado());
    }

    private Usuario usuario() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getIdUsuario()).thenReturn(5L);
        when(usuario.getNombre()).thenReturn("Ana");
        when(usuario.getApellido()).thenReturn("Prueba");
        when(usuario.getCorreo()).thenReturn("ana@tecsup.edu.pe");
        when(usuario.getContrasenaHash()).thenReturn("hash");
        when(usuario.getFechaNacimiento()).thenReturn(LocalDate.of(2000, 1, 1));
        return usuario;
    }
}
