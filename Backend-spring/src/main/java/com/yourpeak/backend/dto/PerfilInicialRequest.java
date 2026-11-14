package com.yourpeak.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Datos que el cliente completa al configurar su perfil por primera vez. */
public record PerfilInicialRequest(
        @NotNull(message = "Selecciona tu objetivo.")
        Integer idTipoObjetivo,

        @NotNull(message = "Selecciona tu nivel de experiencia.")
        Integer idNivelExperiencia,

        @NotNull(message = "Selecciona tu nivel de actividad.")
        Integer idNivelActividad,

        @NotNull(message = "Ingresa tu peso.")
        @DecimalMin(value = "0.01", message = "El peso debe ser mayor que cero.")
        @Digits(integer = 3, fraction = 2, message = "El peso debe tener hasta 3 enteros y 2 decimales.")
        BigDecimal pesoKg,

        @NotNull(message = "Ingresa tu talla.")
        @DecimalMin(value = "0.01", message = "La talla debe ser mayor que cero.")
        @Digits(integer = 3, fraction = 2, message = "La talla debe tener hasta 3 enteros y 2 decimales.")
        BigDecimal tallaCm
) { }
