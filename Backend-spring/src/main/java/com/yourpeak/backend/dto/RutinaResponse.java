package com.yourpeak.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record RutinaResponse(List<DiaRutinaResponse> dias) {
    /** realizadosHoy: idPlanEjercicio de los ejercicios que el cliente ya completó hoy (hora de Lima). */
    public record Consulta(List<DiaRutinaResponse> dias, List<Actividad> actividadReciente, List<Long> realizadosHoy) { }
    public record Actividad(LocalDate fecha, String titulo, int ejercicios) { }
}
