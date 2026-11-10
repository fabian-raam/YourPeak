package com.yourpeak.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** Datos de un día; las horas solo se envían si está disponible. */
public record DiaDisponibilidadRequest(
        @NotNull(message = "Indica el día de la semana.")
        Integer diaSemana,

        @NotNull(message = "Indica si el día está disponible.")
        Boolean disponible,

        LocalTime horaInicio,

        LocalTime horaFin
) {
}
