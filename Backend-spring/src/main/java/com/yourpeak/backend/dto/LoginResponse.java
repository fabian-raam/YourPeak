package com.yourpeak.backend.dto;

public record LoginResponse(
        Long idUsuario,
        String nombre,
        String apellido,
        String correo,
        String rol,
        String estado
) {
}
