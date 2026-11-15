package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.RegistrarIngestaRequest;
import com.yourpeak.backend.service.NutricionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Endpoints internos de nutrición invocados por Django tras validar la sesión. */
@RestController
@RequestMapping("/api/cliente/nutricion")
public class NutricionController {
    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final NutricionService nutricionService;

    public NutricionController(NutricionService nutricionService) {
        this.nutricionService = nutricionService;
    }

    @GetMapping("/alimentos")
    public ResponseEntity<?> listarAlimentos(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        return error != null ? error : ResponseEntity.ok(nutricionService.listarAlimentos(idUsuario));
    }

    @GetMapping
    public ResponseEntity<?> consultarDia(
            @RequestParam(required = false) String fecha,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        if (error != null) return error;
        try {
            return ResponseEntity.ok(nutricionService.consultarDia(
                    idUsuario, fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha)));
        } catch (DateTimeParseException exception) {
            return ResponseEntity.badRequest().body(Map.of("detail", "La fecha debe tener formato YYYY-MM-DD."));
        }
    }

    @PostMapping("/ingestas")
    public ResponseEntity<?> registrar(
            @Valid @RequestBody RegistrarIngestaRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        return error != null ? error : ResponseEntity.status(HttpStatus.CREATED)
                .body(nutricionService.registrar(idUsuario, request));
    }

    @PutMapping("/ingestas/{idIngesta}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long idIngesta,
            @Valid @RequestBody RegistrarIngestaRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        return error != null ? error : ResponseEntity.ok(
                nutricionService.actualizar(idUsuario, idIngesta, request));
    }

    @DeleteMapping("/ingestas/{idIngesta}")
    public ResponseEntity<?> eliminar(
            @PathVariable Long idIngesta,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Client-User-Id", required = false) Long idUsuario) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuario);
        if (error != null) return error;
        nutricionService.eliminar(idUsuario, idIngesta);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Map<String, String>> validarSolicitud(String apiKey, Long idUsuario) {
        if (internalApiKey.isBlank() || apiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("detail", "Acceso no autorizado."));
        }
        if (idUsuario == null) return ResponseEntity.badRequest()
                .body(Map.of("detail", "Falta identificar al cliente."));
        return null;
    }
}
