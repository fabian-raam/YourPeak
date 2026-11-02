package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.MembresiaResponse;
import com.yourpeak.backend.dto.RegistrarMembresiaRequest;
import com.yourpeak.backend.model.Membresia;
import com.yourpeak.backend.service.MembresiaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/clientes/{idCliente}/membresias")
public class MembresiaController {

    private static final ZoneId ZONA_GIMNASIO = ZoneId.of("America/Lima");

    @Value("${yourpeak.internal-api-key:}")
    private String internalApiKey;

    private final MembresiaService membresiaService;

    public MembresiaController(MembresiaService membresiaService) {
        this.membresiaService = membresiaService;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @PathVariable Long idCliente,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idUsuarioAdministrador
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuarioAdministrador);
        if (error != null) return error;
        return ResponseEntity.ok(membresiaService.listarPeriodos(idCliente, idUsuarioAdministrador));
    }

    @PostMapping
    public ResponseEntity<?> renovar(
            @PathVariable Long idCliente,
            @Valid @RequestBody RegistrarMembresiaRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Admin-User-Id", required = false) Long idUsuarioAdministrador
    ) {
        ResponseEntity<Map<String, String>> error = validarSolicitud(apiKey, idUsuarioAdministrador);
        if (error != null) return error;

        Membresia periodo = membresiaService.registrarPeriodo(
                idCliente,
                request.idPlanMembresia(),
                idUsuarioAdministrador,
                request.observacion()
        );
        LocalDate hoy = LocalDate.now(ZONA_GIMNASIO);
        String estado = hoy.isBefore(periodo.getFechaInicio()) ? "PROGRAMADA" : "VIGENTE";

        return ResponseEntity.status(HttpStatus.CREATED).body(new MembresiaResponse(
                periodo.getIdMembresia(),
                periodo.getPlanMembresia().getIdPlanMembresia(),
                periodo.getPlanMembresia().getNombre(),
                periodo.getFechaInicio(),
                periodo.getFechaVencimiento(),
                estado,
                periodo.getObservacion()
        ));
    }

    private ResponseEntity<Map<String, String>> validarSolicitud(
            String apiKey,
            Long idUsuarioAdministrador
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
        return null;
    }
}
