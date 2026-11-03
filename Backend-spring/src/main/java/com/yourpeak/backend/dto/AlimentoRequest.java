package com.yourpeak.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AlimentoRequest(
        @NotBlank(message = "El nombre del alimento es obligatorio.")
        @Size(max = 120, message = "El nombre admite hasta 120 caracteres.")
        String nombre,

        @NotNull(message = "Ingresa la porción base en gramos.")
        @DecimalMin(value = "0.00", inclusive = false, message = "La porción base debe ser mayor que cero.")
        @Digits(integer = 4, fraction = 2, message = "La porción admite hasta 4 dígitos enteros y 2 decimales.")
        BigDecimal porcionBaseG,

        @NotNull(message = "Ingresa las kilocalorías de la porción.")
        @DecimalMin(value = "0.00", message = "Las kilocalorías no pueden ser negativas.")
        @Digits(integer = 5, fraction = 2, message = "Las kilocalorías admiten hasta 5 dígitos enteros y 2 decimales.")
        BigDecimal kcal,

        @NotNull(message = "Ingresa los gramos de proteína.")
        @DecimalMin(value = "0.00", message = "La proteína no puede ser negativa.")
        @Digits(integer = 4, fraction = 2, message = "La proteína admite hasta 4 dígitos enteros y 2 decimales.")
        BigDecimal proteinaG,

        @NotNull(message = "Ingresa los gramos de carbohidratos.")
        @DecimalMin(value = "0.00", message = "Los carbohidratos no pueden ser negativos.")
        @Digits(integer = 4, fraction = 2, message = "Los carbohidratos admiten hasta 4 dígitos enteros y 2 decimales.")
        BigDecimal carbohidratoG,

        @NotNull(message = "Ingresa los gramos de grasa.")
        @DecimalMin(value = "0.00", message = "La grasa no puede ser negativa.")
        @Digits(integer = 4, fraction = 2, message = "La grasa admite hasta 4 dígitos enteros y 2 decimales.")
        BigDecimal grasaG
) {
    public AlimentoRequest {
        nombre = nombre == null ? null : nombre.strip();
    }
}
