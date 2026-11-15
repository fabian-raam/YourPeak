package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.RegistroProgresoRequest;
import com.yourpeak.backend.service.ProgresoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** Endpoints internos de progreso, invocados por Django tras validar la sesión. */
@RestController
@RequestMapping("/api/cliente/progreso")
public class ProgresoController {
    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final ProgresoService progresoService;

    public ProgresoController(ProgresoService progresoService) {
        this.progresoService = progresoService;
    }

    @GetMapping
    public ResponseEntity<?> consultarHistorial(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        return error != null ? error : ResponseEntity.ok(progresoService.consultarHistorial(idUsuario));
    }

    @PostMapping("/peso")
    public ResponseEntity<?> registrarPeso(
            @Valid @RequestBody RegistroProgresoRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        return error != null ? error : ResponseEntity.status(HttpStatus.CREATED)
                .body(progresoService.registrarPeso(idUsuario, request));
    }

    private ResponseEntity<Map<String, String>> validarSolicitud(String apiKey, Long idUsuario) {
        if (internalApiKey.isBlank() || apiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("detail", "Acceso no autorizado."));
        }
        if (idUsuario == null) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Falta identificar al cliente."));
        }
        return null;
    }
}
