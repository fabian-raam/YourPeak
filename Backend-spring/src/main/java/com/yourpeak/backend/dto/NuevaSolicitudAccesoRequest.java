package com.yourpeak.backend.dto;

import jakarta.validation.constraints.Size;

/** Mensaje opcional que el cliente deja al volver a pedir acceso. */
public record NuevaSolicitudAccesoRequest(
        @Size(max = 300, message = "El mensaje admite hasta 300 caracteres.") String mensaje
) { }
