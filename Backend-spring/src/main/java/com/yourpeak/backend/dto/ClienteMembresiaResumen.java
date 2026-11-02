package com.yourpeak.backend.dto;

import java.time.LocalDate;

public record ClienteMembresiaResumen(
        Long idCliente,
        String nombre,
        String apellido,
        String correo,
        String estadoUsuario,
        LocalDate fechaVencimiento,
        String estadoMembresia
) {
}
