package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.AlimentoRequest;
import com.yourpeak.backend.service.AlimentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/admin/alimentos")
public class AlimentoController {
    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final AlimentoService alimentoService;

    public AlimentoController(AlimentoService alimentoService) {
        this.alimentoService = alimentoService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
                                    @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminId) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, adminId);
        return error != null ? error : ResponseEntity.ok(alimentoService.listar(adminId));
    }

    @GetMapping("/{idAlimento}")
    public ResponseEntity<?> obtener(@PathVariable Integer idAlimento,
                                     @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
                                     @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminId) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, adminId);
        return error != null ? error : ResponseEntity.ok(alimentoService.obtener(idAlimento, adminId));
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody AlimentoRequest request,
                                   @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
                                   @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminId) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, adminId);
        if (error != null) return error;
        return ResponseEntity.status(HttpStatus.CREATED).body(alimentoService.crear(request, adminId));
    }

    @PutMapping("/{idAlimento}")
    public ResponseEntity<?> actualizar(@PathVariable Integer idAlimento,
                                        @Valid @RequestBody AlimentoRequest request,
                                        @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
                                        @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminId) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, adminId);
        if (error != null) return error;
        return ResponseEntity.ok(alimentoService.actualizar(idAlimento, request, adminId));
    }

    private ResponseEntity<Map<String, String>> validarSolicitud(String apiKey, Long adminId) {
        if (internalApiKey.isBlank() || apiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("detail", "Acceso no autorizado."));
        }
        if (adminId == null) return ResponseEntity.badRequest()
                .body(Map.of("detail", "Falta identificar al administrador."));
        return null;
    }
}
