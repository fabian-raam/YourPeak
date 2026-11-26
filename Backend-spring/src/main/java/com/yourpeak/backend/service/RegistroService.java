package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.GoogleAuthRequest;
import com.yourpeak.backend.dto.RegistroRequest;
import com.yourpeak.backend.dto.RegistroResponse;
import com.yourpeak.backend.exception.CorreoRegistradoException;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.model.Usuario;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import com.yourpeak.backend.repository.UsuarioRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Validated
public class RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final TipoEstadoRepository tipoEstadoRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroService(
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            TipoEstadoRepository tipoEstadoRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.tipoEstadoRepository = tipoEstadoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistroResponse registrar(
            @NotNull @Valid RegistroRequest request
    ) {
        return crearCliente(request.nombre(), request.apellido(), request.correo(), request.fechaNacimiento(),
                request.contrasena(), null);
    }

    @Transactional
    public RegistroResponse registrarConGoogle(@NotNull @Valid GoogleAuthRequest request) {
        // La contraseña aleatoria no se entrega al usuario ni se almacena en texto plano.
        return crearCliente(request.nombre().strip(), request.apellido().strip(), request.correo(),
                request.fechaNacimiento(), UUID.randomUUID().toString(), request.subject());
    }

    private RegistroResponse crearCliente(String nombre, String apellido, String correo, LocalDate nacimiento,
                                          String contrasena, String googleSubject) {
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new CorreoRegistradoException();
        }

        TipoEstado pendiente = tipoEstadoRepository
                .findByNombre("pendiente")
                .orElseThrow(() -> new IllegalStateException(
                        "Falta configurar el estado pendiente"
                ));

        String hash = passwordEncoder.encode(contrasena);

        Usuario usuario = new Usuario(
                nombre, apellido, correo, hash, nacimiento
        );
        if (googleSubject != null) {
            usuario.vincularGoogle(googleSubject);
        }

        usuarioRepository.save(usuario);

        Cliente cliente = new Cliente(usuario, pendiente);
        clienteRepository.saveAndFlush(cliente);

        return new RegistroResponse(
                usuario.getIdUsuario(),
                cliente.getIdCliente(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                pendiente.getNombre(),
                "Registro exitoso. Tu cuenta está pendiente de aprobación."
        );
    }
}