package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.PerfilInicialRequest;
import com.yourpeak.backend.service.PerfilClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** Endpoints internos para que Django lea y guarde el perfil del cliente autenticado. */
@RestController
@RequestMapping("/api/cliente/perfil")
public class PerfilClienteController {

    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final PerfilClienteService perfilClienteService;

    public PerfilClienteController(PerfilClienteService perfilClienteService) {
        this.perfilClienteService = perfilClienteService;
    }

    @GetMapping
    public ResponseEntity<?> consultar(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        return error != null ? error : ResponseEntity.ok(perfilClienteService.consultar(idUsuario));
    }

    @PutMapping
    public ResponseEntity<?> guardar(
            @Valid @RequestBody PerfilInicialRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        return error != null ? error : ResponseEntity.ok(perfilClienteService.guardar(idUsuario, request));
    }

    private ResponseEntity<Map<String, String>> validarSolicitud(String apiKey, Long idUsuario) {
        if (internalApiKey.isBlank() || apiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8)
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("detail", "Acceso no autorizado."));
        }
        if (idUsuario == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("detail", "Falta identificar al cliente."));
        }
        return null;
    }
}
