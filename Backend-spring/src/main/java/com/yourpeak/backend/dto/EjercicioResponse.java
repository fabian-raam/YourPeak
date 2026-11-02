package com.yourpeak.backend.dto;

public record EjercicioResponse(
        Integer idEjercicio,
        Integer idGrupoMuscular,
        String grupoMuscular,
        Integer idNivelExperiencia,
        String nivelExperiencia,
        Short idTipoMedicion,
        String tipoMedicion,
        String nombre,
        String descripcion,
        String equipamiento,
        String imagenRuta,
        String estado
) { }
