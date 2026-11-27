package com.yourpeak.backend.controller;

import org.springframework.web.bind.annotation.RequestParam;

import com.yourpeak.backend.service.PanelAdministradorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** Endpoints de solo lectura para Inicio y Entrenamientos del panel de administración. */
@RestController
@RequestMapping("/api/admin")
public class PanelAdministradorController {
    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final PanelAdministradorService panelService;

    public PanelAdministradorController(PanelAdministradorService panelService) {
        this.panelService = panelService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<?> consultarResumen(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idAdministrador) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idAdministrador);
        return error != null ? error : ResponseEntity.ok(panelService.consultarResumen(idAdministrador));
    }

    @GetMapping("/actividad")
    public ResponseEntity<?> consultarActividad(
            @RequestParam(defaultValue = "14") int dias,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idAdministrador) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idAdministrador);
        return error != null ? error : ResponseEntity.ok(panelService.consultarActividad(idAdministrador, dias));
    }

    @GetMapping("/entrenamientos")
    public ResponseEntity<?> listarEntrenamientos(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idAdministrador) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idAdministrador);
        return error != null ? error : ResponseEntity.ok(panelService.listarEntrenamientos(idAdministrador));
    }

    @GetMapping("/entrenamientos/{idCliente}")
    public ResponseEntity<?> consultarEntrenamiento(
            @PathVariable Long idCliente,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idAdministrador) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idAdministrador);
        return error != null ? error
                : ResponseEntity.ok(panelService.consultarEntrenamiento(idCliente, idAdministrador));
    }

    private ResponseEntity<Map<String, String>> validarSolicitud(String apiKey, Long idAdministrador) {
        if (internalApiKey.isBlank() || apiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
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
