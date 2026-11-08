package com.yourpeak.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DisponibilidadRequest(
        @NotNull(message = "Envía la disponibilidad de la semana.")
        List<@Valid DiaDisponibilidadRequest> dias
) {
}
