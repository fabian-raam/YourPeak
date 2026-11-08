package com.yourpeak.backend.dto;

import java.time.LocalTime;
import java.util.List;

public record DiaRutinaResponse(
        Integer diaSemana,
        Boolean disponible,
        LocalTime horaInicio,
        LocalTime horaFin,
        List<EjercicioRutinaResponse> ejercicios
) { }
