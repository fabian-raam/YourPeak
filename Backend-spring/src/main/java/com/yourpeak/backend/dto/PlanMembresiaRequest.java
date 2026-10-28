package com.yourpeak.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanMembresiaRequest(
        @NotBlank(message = "El nombre del plan es obligatorio.")
        @Size(max = 60, message = "El nombre admite hasta 60 caracteres.")
        String nombre,

        @NotNull(message = "La duración es obligatoria.")
        @Min(value = 1, message = "La duración mínima es 1 mes.")
        @Max(value = 127, message = "La duración máxima es 127 meses.")
        Integer duracionMeses
) {
    public PlanMembresiaRequest {
        nombre = nombre == null ? null : nombre.strip();
    }
}
