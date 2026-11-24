package com.yourpeak.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Valores de una prescripción; repeticiones o segundos depende del tipo de medición del ejercicio. */
public record EjercicioRutinaRequest(
        @NotNull(message = "Selecciona un ejercicio.")
        Integer idEjercicio,

        @NotNull(message = "Indica cuántas series realizar.") @Min(1) @Max(100)
        Integer series,

        @Min(1) @Max(127)
        Integer repeticiones,

        @Min(1) @Max(32767)
        Integer segundos,

        @NotNull(message = "Indica el descanso entre series.") @Min(0) @Max(32767)
        Integer descansoSegundos,

        /** IA si lo propuso el generador y el cliente no lo cambió; USUARIO (o vacío) si lo agregó el cliente. */
        @Pattern(regexp = "IA|USUARIO", message = "El origen debe ser IA o USUARIO.")
        String origen
) { }
