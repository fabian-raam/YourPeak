package com.yourpeak.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Rutina semanal actual y actividad reciente de un cliente. */
public record DetalleEntrenamientoAdministradorResponse(
        Long idCliente,
        String nombre,
        String apellido,
        String correo,
        String estado,
        LocalDateTime fechaRutina,
        List<DiaRutinaResponse> dias,
        List<RutinaResponse.Actividad> actividadReciente
) { }
