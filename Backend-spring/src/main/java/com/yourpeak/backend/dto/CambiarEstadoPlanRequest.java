package com.yourpeak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarEstadoPlanRequest(
        @NotBlank(message = "El estado es obligatorio.")
        @Pattern(regexp = "(?i)ACTIVO|INACTIVO", message = "El estado debe ser ACTIVO o INACTIVO.")
        String estado
) {
    public CambiarEstadoPlanRequest {
        estado = estado == null ? null : estado.strip().toUpperCase();
    }
}
