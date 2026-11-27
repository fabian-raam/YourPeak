package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.NuevaSolicitudAccesoRequest;
import com.yourpeak.backend.service.SolicitudAccesoService;
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

/** Estado de la solicitud de acceso para clientes pendientes o rechazados (vía Django). */
@RestController
@RequestMapping("/api/cliente/solicitud")
public class SolicitudAccesoController {

    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final SolicitudAccesoService solicitudService;

    public SolicitudAccesoController(SolicitudAccesoService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public ResponseEntity<?> consultar(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        return error != null ? error : ResponseEntity.ok(solicitudService.consultar(idUsuario));
    }

    @PostMapping
    public ResponseEntity<?> volverASolicitar(
            @Valid @RequestBody NuevaSolicitudAccesoRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        return error != null ? error
                : ResponseEntity.ok(solicitudService.volverASolicitar(idUsuario, request.mensaje()));
    }

    private ResponseEntity<Map<String, String>> validarSolicitud(String apiKey, Long idUsuario) {
        if (internalApiKey.isBlank() || apiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("detail", "Acceso no autorizado."));
        }
        if (idUsuario == null) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Falta identificar al cliente."));
        }
        return null;
    }
}
