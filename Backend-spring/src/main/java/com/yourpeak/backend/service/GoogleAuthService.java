package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.GoogleAuthRequest;
import com.yourpeak.backend.dto.LoginResponse;
import com.yourpeak.backend.model.Usuario;
import com.yourpeak.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleAuthService {
    private final UsuarioRepository usuarios;
    private final RegistroService registro;
    private final LoginService login;
    private final PasswordEncoder passwordEncoder;

    public GoogleAuthService(UsuarioRepository usuarios, RegistroService registro, LoginService login,
                             PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.registro = registro;
        this.login = login;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Result authenticate(GoogleAuthRequest request) {
        Usuario vinculado = usuarios.findByGoogleSubject(request.subject()).orElse(null);
        if (vinculado != null) {
            if (!vinculado.getCorreo().equalsIgnoreCase(request.correo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El correo de tu cuenta de Google cambió. Contacta al equipo de YourPeak.");
            }
            return autenticado(vinculado.getIdUsuario());
        }

        Usuario existente = usuarios.findByCorreo(request.correo()).orElse(null);
        if (existente != null) {
            if (existente.getGoogleSubject() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Tu cuenta de YourPeak ya está vinculada a otra cuenta de Google.");
            }
            if (request.contrasena() == null) {
                return new Result("vincular", null);
            }
            if (request.contrasena().isBlank()
                    || !passwordEncoder.matches(request.contrasena(), existente.getContrasenaHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contraseña es incorrecta.");
            }
            int actualizados = usuarios.vincularGoogle(existente.getIdUsuario(), request.correo(), request.subject());
            if (actualizados == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Vuelve a iniciar sesión con Google para continuar.");
            }
            return autenticado(existente.getIdUsuario());
        }

        if (request.fechaNacimiento() == null) {
            return new Result("completar", null);
        }
        if (request.nombre() == null || request.nombre().isBlank()
                || request.apellido() == null || request.apellido().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa tu nombre y apellido.");
        }
        return autenticado(registro.registrarConGoogle(request).idUsuario());
    }

    private Result autenticado(Long idUsuario) {
        return new Result("autenticado", login.buscarUsuario(idUsuario).orElseThrow());
    }

    public record Result(String estado, LoginResponse usuario) {}
}
