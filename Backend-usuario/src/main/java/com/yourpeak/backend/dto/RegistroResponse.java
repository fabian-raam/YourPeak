package com.yourpeak.backend.dto;

public record RegistroResponse(
        Long idUsuario,
        Long idCliente,
        String nombre,
        String apellido,
        String correo,
        String estado,
        String mensaje
) {
}