package com.yourpeak.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Peso que el cliente registra en una nueva medición de progreso. */
public record RegistroProgresoRequest(
        @NotNull(message = "Ingresa tu peso.")
        @DecimalMin(value = "0.01", message = "El peso debe ser mayor que cero.")
        @Digits(integer = 3, fraction = 2, message = "El peso debe tener hasta 3 enteros y 2 decimales.")
        BigDecimal pesoKg
) { }
