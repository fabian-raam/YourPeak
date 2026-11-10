package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.DisponibilidadRequest;
import com.yourpeak.backend.service.DisponibilidadService;
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

/** Endpoints internos para que Django consulte o guarde la disponibilidad del cliente en sesión. */
@RestController
@RequestMapping("/api/cliente/disponibilidad")
public class DisponibilidadController {

    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping
    public ResponseEntity<?> consultar(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        return error != null ? error : ResponseEntity.ok(disponibilidadService.consultar(idUsuario));
    }

    @PutMapping
    public ResponseEntity<?> actualizar(
            @Valid @RequestBody DisponibilidadRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        return error != null ? error : ResponseEntity.ok(
                disponibilidadService.actualizar(idUsuario, request)
        );
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
