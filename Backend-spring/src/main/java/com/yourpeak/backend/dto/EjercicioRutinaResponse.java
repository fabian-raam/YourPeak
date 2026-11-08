package com.yourpeak.backend.dto;

public record EjercicioRutinaResponse(
        Integer idEjercicio,
        String nombre,
        String grupoMuscular,
        String tipoMedicion,
        Integer orden,
        Integer series,
        Integer repeticiones,
        Integer segundos,
        Integer descansoSegundos,
        String origen
) { }
