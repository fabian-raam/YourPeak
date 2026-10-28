package com.yourpeak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarNombrePlanRequest(
        @NotBlank(message = "El nombre del plan es obligatorio.")
        @Size(max = 60, message = "El nombre admite hasta 60 caracteres.")
        String nombre
) {
    public CambiarNombrePlanRequest {
        nombre = nombre == null ? null : nombre.strip();
    }
}
