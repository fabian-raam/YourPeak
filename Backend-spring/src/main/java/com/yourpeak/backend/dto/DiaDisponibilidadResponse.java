package com.yourpeak.backend.dto;

import java.time.LocalTime;

public record DiaDisponibilidadResponse(
        Integer diaSemana,
        Boolean disponible,
        LocalTime horaInicio,
        LocalTime horaFin
) {
}
