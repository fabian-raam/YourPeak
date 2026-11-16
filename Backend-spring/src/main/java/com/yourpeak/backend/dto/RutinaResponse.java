package com.yourpeak.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record RutinaResponse(List<DiaRutinaResponse> dias) {
    public record Consulta(List<DiaRutinaResponse> dias, List<Actividad> actividadReciente) { }
    public record Actividad(LocalDate fecha, String titulo, int ejercicios) { }
}
