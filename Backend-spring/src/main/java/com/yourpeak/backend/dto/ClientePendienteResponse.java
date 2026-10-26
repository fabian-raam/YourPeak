package com.yourpeak.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Datos seguros que necesita el panel administrativo para revisar un cliente.
 */
public record ClientePendienteResponse(
        Long idCliente,
        Long idUsuario,
        String nombre,
        String apellido,
        String correo,
        LocalDate fechaNacimiento,
        LocalDateTime fechaAlta,
        String estado
) {
}
