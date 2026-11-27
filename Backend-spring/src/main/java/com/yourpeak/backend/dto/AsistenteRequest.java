package com.yourpeak.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Pregunta al asistente y los últimos mensajes de la conversación (el servidor no la guarda). */
public record AsistenteRequest(
        @NotBlank(message = "Escribe tu pregunta.")
        @Size(max = 500, message = "La pregunta admite hasta 500 caracteres.")
        String pregunta,
        @Size(max = 12, message = "El historial admite hasta 12 mensajes.")
        List<@Valid Mensaje> historial
) {
    public record Mensaje(
            @NotBlank @Pattern(regexp = "usuario|asistente", message = "Rol no válido.") String rol,
            @NotBlank @Size(max = 4000, message = "Un mensaje del historial es demasiado largo.") String texto
    ) { }
}
