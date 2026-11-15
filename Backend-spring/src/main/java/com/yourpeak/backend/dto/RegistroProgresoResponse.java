package com.yourpeak.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Medición física guardada en el historial del cliente. */
public record RegistroProgresoResponse(
        Long idRegistroFisico,
        BigDecimal pesoKg,
        BigDecimal tallaCm,
        LocalDateTime fechaRegistro
) { }
