package com.yourpeak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EjercicioRequest(
        @NotNull(message = "Selecciona un grupo muscular.")
        @Positive(message = "El grupo muscular no es válido.")
        Integer idGrupoMuscular,

        @NotNull(message = "Selecciona el nivel mínimo de experiencia.")
        @Positive(message = "El nivel de experiencia no es válido.")
        Integer idNivelExperiencia,

        @NotNull(message = "Selecciona el tipo de medición.")
        @Positive(message = "El tipo de medición no es válido.")
        Short idTipoMedicion,

        @NotBlank(message = "El nombre del ejercicio es obligatorio.")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres.")
        String nombre,

        @Size(max = 500, message = "La descripción admite hasta 500 caracteres.")
        String descripcion,

        @Size(max = 100, message = "El equipamiento admite hasta 100 caracteres.")
        String equipamiento,

        @Size(max = 255, message = "La ruta de imagen admite hasta 255 caracteres.")
        String imagenRuta
) {
    public EjercicioRequest {
        nombre = nombre == null ? null : nombre.strip();
        descripcion = normalizarOpcional(descripcion);
        equipamiento = normalizarOpcional(equipamiento);
        imagenRuta = normalizarOpcional(imagenRuta);
    }

    private static String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.strip();
    }
}
