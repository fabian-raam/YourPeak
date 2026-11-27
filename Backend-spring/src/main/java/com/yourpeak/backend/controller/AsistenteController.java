package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.AsistenteRequest;
import com.yourpeak.backend.service.AsistenteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** Chat con IA sobre alimentación y ejercicio para clientes activos (vía Django o la app móvil). */
@RestController
@RequestMapping("/api/cliente/asistente")
public class AsistenteController {

    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final AsistenteService asistenteService;

    public AsistenteController(AsistenteService asistenteService) {
        this.asistenteService = asistenteService;
    }

    @PostMapping
    public ResponseEntity<?> preguntar(
            @Valid @RequestBody AsistenteRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario) {
        if (internalApiKey.isBlank() || apiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("detail", "Acceso no autorizado."));
        }
        if (idUsuario == null) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Falta identificar al cliente."));
        }
        return ResponseEntity.ok(asistenteService.responder(idUsuario, request));
    }
}
