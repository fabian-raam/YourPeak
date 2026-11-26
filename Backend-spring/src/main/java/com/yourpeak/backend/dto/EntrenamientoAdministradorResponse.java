package com.yourpeak.backend.dto;

import java.time.LocalDateTime;

/** Resumen de rutina y actividad de un cliente para el panel administrativo. */
public record EntrenamientoAdministradorResponse(
        Long idCliente,
        String nombre,
        String apellido,
        String correo,
        String estado,
        LocalDateTime fechaRutina,
        int diasProgramados,
        int ejerciciosProgramados,
        int sesionesUltimos30Dias,
        LocalDateTime ultimaActividad
) { }
