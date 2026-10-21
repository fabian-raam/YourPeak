package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.LoginRequest;
import com.yourpeak.backend.dto.LoginResponse;
import com.yourpeak.backend.model.Usuario;
import com.yourpeak.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<LoginResponse> autenticar(LoginRequest request) {
        Optional<Usuario> usuarioEncontrado = usuarioRepository.findByCorreo(request.correo());

        if (usuarioEncontrado.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarioEncontrado.get();
        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash())) {
            return Optional.empty();
        }

        return Optional.of(new LoginResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo()
        ));
    }

    public Optional<LoginResponse> buscarUsuario(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .map(usuario -> new LoginResponse(
                        usuario.getIdUsuario(),
                        usuario.getNombre(),
                        usuario.getApellido(),
                        usuario.getCorreo()
                ));
    }
}
