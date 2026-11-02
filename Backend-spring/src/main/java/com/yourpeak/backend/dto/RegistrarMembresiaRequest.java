package com.yourpeak.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegistrarMembresiaRequest(
        @NotNull(message = "Debes seleccionar un plan de membresía.")
        @Positive(message = "El identificador del plan debe ser positivo.")
        Integer idPlanMembresia,

        @Size(max = 500, message = "La observación admite hasta 500 caracteres.")
        String observacion
) {
    public RegistrarMembresiaRequest {
        observacion = observacion == null || observacion.isBlank()
                ? null
                : observacion.strip();
    }
}
