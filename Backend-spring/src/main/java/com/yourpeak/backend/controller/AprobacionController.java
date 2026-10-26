package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.ClientePendienteResponse;
import com.yourpeak.backend.service.AprobacionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/clientes")
public class AprobacionController {

    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final AprobacionService aprobacionService;

    public AprobacionController(AprobacionService aprobacionService) {
        this.aprobacionService = aprobacionService;
    }

    /** Devuelve la lista de clientes pendientes para el panel administrativo de Django. */
    @GetMapping("/pendientes")
    public ResponseEntity<?> listarPendientes(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idUsuarioAdministrador
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitudInterna(
                apiKey,
                idUsuarioAdministrador
        );
        if (error != null) {
            return error;
        }

        return ResponseEntity.ok(aprobacionService.listarPendientes(idUsuarioAdministrador));
    }

    /** Devuelve los datos de un cliente pendiente que el administrador seleccionó. */
    @GetMapping("/pendientes/{idCliente}")
    public ResponseEntity<?> obtenerPendiente(
            @PathVariable Long idCliente,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idUsuarioAdministrador
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitudInterna(
                apiKey,
                idUsuarioAdministrador
        );
        if (error != null) {
            return error;
        }

        ClientePendienteResponse response = aprobacionService.obtenerPendiente(
                idCliente,
                idUsuarioAdministrador
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Recibe de Django la solicitud autenticada para aprobar un cliente.
     */
    @PostMapping("/{idCliente}/aprobar")
    public ResponseEntity<Map<String, String>> aprobarCliente(
            @PathVariable Long idCliente,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idUsuarioAdministrador
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitudInterna(
                apiKey,
                idUsuarioAdministrador
        );
        if (error != null) {
            return error;
        }

        aprobacionService.aprobarCliente(idCliente, idUsuarioAdministrador);

        return ResponseEntity.ok(Map.of(
                "detail", "El cliente fue aprobado.",
                "estado", "ACTIVO"
        ));
    }

    /** Valida la clave servicio-a-servicio y que Django haya identificado al administrador. */
    private ResponseEntity<Map<String, String>> validarSolicitudInterna(
            String apiKey,
            Long idUsuarioAdministrador
    ) {
        // La clave secreta permite que solo los backends, no React, llamen este controlador.
        if (internalApiKey.isBlank() || apiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8)
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("detail", "Acceso no autorizado."));
        }

        if (idUsuarioAdministrador == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("detail", "Falta identificar al administrador."));
        }

        return null;
    }
}
