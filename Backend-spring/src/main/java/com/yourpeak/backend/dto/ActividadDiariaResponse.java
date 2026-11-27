package com.yourpeak.backend.dto;

import java.time.LocalDate;

/** Sesiones de entrenamiento (cliente + día con series registradas) de una fecha. */
public record ActividadDiariaResponse(LocalDate fecha, int sesiones) { }
