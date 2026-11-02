package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.ClienteMembresiaResumen;
import com.yourpeak.backend.service.MembresiaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/clientes")
public class ClientesMembresiaController {

    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final MembresiaService membresiaService;

    public ClientesMembresiaController(MembresiaService membresiaService) {
        this.membresiaService = membresiaService;
    }

    @GetMapping("/activos")
    public ResponseEntity<?> listarActivos(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idUsuarioAdministrador
    ) {
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
        List<ClienteMembresiaResumen> clientes = membresiaService
                .listarClientesActivos(idUsuarioAdministrador);
        return ResponseEntity.ok(clientes);
    }
}
