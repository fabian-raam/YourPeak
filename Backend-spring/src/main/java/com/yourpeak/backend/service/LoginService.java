package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.LoginRequest;
import com.yourpeak.backend.dto.LoginResponse;
import com.yourpeak.backend.model.Usuario;
import com.yourpeak.backend.repository.AdministradorRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.MembresiaRepository;
import com.yourpeak.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class LoginService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final AdministradorRepository administradorRepository;
    private final MembresiaRepository membresiaRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginService(
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            AdministradorRepository administradorRepository,
            MembresiaRepository membresiaRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.administradorRepository = administradorRepository;
        this.membresiaRepository = membresiaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<LoginResponse> autenticar(LoginRequest request) {
        Optional<Usuario> usuarioEncontrado = usuarioRepository.findByCorreo(request.correo());

        if (usuarioEncontrado.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarioEncontrado.get();
        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash())) {
            return Optional.empty();
        }

        return Optional.of(crearRespuesta(usuario));
    }

    @Transactional(readOnly = true)
    public Optional<LoginResponse> buscarUsuario(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .map(this::crearRespuesta);
    }

    /** Incluye el estado del cliente para que la aplicación muestre el acceso correcto. */
    private LoginResponse crearRespuesta(Usuario usuario) {
        // Si la cuenta también es cliente, el rol administrativo tiene prioridad al iniciar sesión.
        if (administradorRepository.existsByUsuario_IdUsuario(usuario.getIdUsuario())) {
            return new LoginResponse(
                    usuario.getIdUsuario(),
                    usuario.getNombre(),
                    usuario.getApellido(),
                    usuario.getCorreo(),
                    "ADMINISTRADOR",
                    null,
                    null
            );
        }

        // El rol CLIENTE y su estado se obtienen del perfil asociado, si existe.
        var cliente = clienteRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        String estado = cliente.map(item -> item.getTipoEstado().getNombre()).orElse(null);
        String rol = cliente.isPresent() ? "CLIENTE" : null;
        boolean membresiaVigente = cliente
                .filter(item -> "ACTIVO".equalsIgnoreCase(item.getTipoEstado().getNombre()))
                .map(item -> membresiaRepository
                        .existsByCliente_IdClienteAndFechaInicioLessThanEqualAndFechaVencimientoGreaterThanEqual(
                                item.getIdCliente(),
                                LocalDate.now(ZoneId.of("America/Lima")),
                                LocalDate.now(ZoneId.of("America/Lima"))
                        ))
                .orElse(false);

        return new LoginResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                rol,
                estado,
                membresiaVigente
        );
    }
}
