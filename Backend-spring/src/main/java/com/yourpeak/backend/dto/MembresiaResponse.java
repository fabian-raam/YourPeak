package com.yourpeak.backend.dto;

import java.time.LocalDate;

public record MembresiaResponse(
        Long idMembresia,
        Integer idPlanMembresia,
        String nombrePlan,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        String estadoPeriodo,
        String observacion
) {
}
