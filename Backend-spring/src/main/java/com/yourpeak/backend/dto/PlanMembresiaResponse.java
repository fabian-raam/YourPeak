package com.yourpeak.backend.dto;

public record PlanMembresiaResponse(
        Integer idPlanMembresia,
        String nombre,
        Integer duracionMeses,
        String estado
) {
}
