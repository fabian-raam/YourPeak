package com.yourpeak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo que verá el cliente cuando el administrador rechaza su solicitud. */
public record RechazarClienteRequest(
        @NotBlank(message = "Indica el motivo del rechazo.")
        @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres.")
        String motivo
) { }
