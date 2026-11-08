package com.yourpeak.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Reemplaza la rutina semanal activa con los ejercicios ordenados de cada día. */
public record RutinaRequest(
        @NotNull(message = "Envía los siete días de la rutina.")
        @Size(min = 7, max = 7, message = "Envía exactamente los siete días de la semana.")
        List<@NotNull @Valid DiaRutinaRequest> dias
) { }
