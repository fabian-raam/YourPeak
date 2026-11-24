package com.yourpeak.backend.dto;

import java.time.LocalDate;

public record EjercicioRealizadoResponse(
        Long idPlanEjercicio,
        String ejercicio,
        LocalDate fecha,
        int seriesCompletadas,
        boolean completado,
        String detail
) { }
