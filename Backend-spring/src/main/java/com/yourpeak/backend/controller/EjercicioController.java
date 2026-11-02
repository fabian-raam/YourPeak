package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.CambiarEstadoEjercicioRequest;
import com.yourpeak.backend.dto.EjercicioRequest;
import com.yourpeak.backend.service.EjercicioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ejercicios")
public class EjercicioController {
    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final EjercicioService ejercicioService;

    public EjercicioController(EjercicioService ejercicioService) {
        this.ejercicioService = ejercicioService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
                                    @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminId) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, adminId);
        return error != null ? error : ResponseEntity.ok(ejercicioService.listar(adminId));
    }

    @GetMapping("/catalogos")
    public ResponseEntity<?> catalogos(@RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
                                       @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminId) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, adminId);
        return error != null ? error : ResponseEntity.ok(ejercicioService.catalogos(adminId));
    }

    @GetMapping("/{idEjercicio}")
    public ResponseEntity<?> obtener(@PathVariable Integer idEjercicio,
                                     @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
                                     @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminId) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, adminId);
        return error != null ? error : ResponseEntity.ok(ejercicioService.obtener(idEjercicio, adminId));
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody EjercicioRequest request,
                                   @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
                                   @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminId) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, adminId);
        if (error != null) return error;
        return ResponseEntity.status(HttpStatus.CREATED).body(ejercicioService.crear(request, adminId));
    }

    @PutMapping("/{idEjercicio}")
    public ResponseEntity<?> actualizar(@PathVariable Integer idEjercicio,
                                        @Valid @RequestBody EjercicioRequest request,
                                        @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
                                        @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminId) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, adminId);
        if (error != null) return error;
        return ResponseEntity.ok(ejercicioService.actualizar(idEjercicio, request, adminId));
    }

    @PatchMapping("/{idEjercicio}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Integer idEjercicio,
                                           @Valid @RequestBody CambiarEstadoEjercicioRequest request,
                                           @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
                                           @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminId) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, adminId);
        if (error != null) return error;
        return ResponseEntity.ok(ejercicioService.cambiarEstado(idEjercicio, request, adminId));
    }

    private ResponseEntity<Map<String, String>> validarSolicitud(String apiKey, Long adminId) {
        if (internalApiKey.isBlank() || apiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("detail", "Acceso no autorizado."));
        }
        if (adminId == null) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Falta identificar al administrador."));
        }
        return null;
    }
}
