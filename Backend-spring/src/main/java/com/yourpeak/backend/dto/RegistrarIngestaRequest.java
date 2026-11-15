package com.yourpeak.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Entrada para anotar una porción consumida; el cliente se obtiene del header validado. */
public record RegistrarIngestaRequest(
        @NotNull(message = "Selecciona un alimento.") Integer idAlimento,
        @NotNull(message = "Indica la cantidad consumida en gramos.")
        @DecimalMin(value = "0.00", inclusive = false, message = "La cantidad debe ser mayor que cero.")
        @Digits(integer = 5, fraction = 2, message = "La cantidad admite hasta 5 enteros y 2 decimales.")
        BigDecimal cantidadG
) { }
