package com.yourpeak.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Series completadas hoy; usa repeticiones o segundos según el ejercicio. */
public record RegistrarEjercicioRealizadoRequest(
        @NotEmpty(message = "Registra las series que completaste.")
        @Size(max = 100, message = "La cantidad de series excede la permitida.")
        List<@NotNull @Valid Serie> series
) {
    public record Serie(
            @NotNull @Min(1) @Max(100) Integer numeroSerie,
            @Min(1) @Max(127) Integer repeticiones,
            @Min(1) @Max(32767) Integer segundos
    ) { }
}
