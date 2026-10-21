package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.LoginRequest;
import com.yourpeak.backend.dto.LoginResponse;
import com.yourpeak.backend.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return loginService.autenticar(request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of(
                                "detail", "Correo o contraseña incorrectos."
                        )));
    }

    @GetMapping("/usuarios/{idUsuario}")
    public ResponseEntity<?> obtenerUsuario(
            @PathVariable Long idUsuario,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey
    ) {
        if (internalApiKey.isBlank() || apiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8)
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("detail", "Acceso no autorizado."));
        }

        return loginService.buscarUsuario(idUsuario)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(java.util.Map.of("detail", "Usuario no encontrado.")));
    }
}
