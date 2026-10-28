package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.CambiarEstadoPlanRequest;
import com.yourpeak.backend.dto.CambiarNombrePlanRequest;
import com.yourpeak.backend.dto.PlanMembresiaRequest;
import com.yourpeak.backend.service.PlanMembresiaService;
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
@RequestMapping("/api/admin/planes-membresia")
public class PlanMembresiaController {

    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final PlanMembresiaService planService;

    public PlanMembresiaController(PlanMembresiaService planService) {
        this.planService = planService;
    }

    /** Devuelve planes activos e inactivos para que el administrador los gestione. */
    @GetMapping
    public ResponseEntity<?> listar(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idAdministrador
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idAdministrador);
        if (error != null) {
            return error;
        }
        return ResponseEntity.ok(planService.listar(idAdministrador));
    }

    /** Devuelve un plan por su identificador. */
    @GetMapping("/{idPlan}")
    public ResponseEntity<?> obtener(
            @PathVariable Integer idPlan,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idAdministrador
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idAdministrador);
        if (error != null) {
            return error;
        }
        return ResponseEntity.ok(planService.obtener(idPlan, idAdministrador));
    }

    /** Crea un plan nuevo activo, si cumple las validaciones del contrato. */
    @PostMapping
    public ResponseEntity<?> crear(
            @Valid @RequestBody PlanMembresiaRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idAdministrador
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idAdministrador);
        if (error != null) {
            return error;
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.crear(request, idAdministrador));
    }

    /** Edita solo el nombre; la duración del plan queda fija después de su creación. */
    @PutMapping("/{idPlan}")
    public ResponseEntity<?> actualizar(
            @PathVariable Integer idPlan,
            @Valid @RequestBody CambiarNombrePlanRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idAdministrador
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idAdministrador);
        if (error != null) {
            return error;
        }
        return ResponseEntity.ok(planService.actualizar(idPlan, request, idAdministrador));
    }

    /** Cambia únicamente entre ACTIVO e INACTIVO; no borra el plan ni su historial. */
    @PatchMapping("/{idPlan}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Integer idPlan,
            @Valid @RequestBody CambiarEstadoPlanRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idAdministrador
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idAdministrador);
        if (error != null) {
            return error;
        }
        return ResponseEntity.ok(planService.cambiarEstado(idPlan, request, idAdministrador));
    }

    /** Comprueba la credencial interna; el servicio también verifica el rol en MySQL. */
    private ResponseEntity<Map<String, String>> validarSolicitud(
            String apiKey,
            Long idAdministrador
    ) {
        if (internalApiKey.isBlank() || apiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8)
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("detail", "Acceso no autorizado."));
        }
        if (idAdministrador == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("detail", "Falta identificar al administrador."));
        }
        return null;
    }
}
