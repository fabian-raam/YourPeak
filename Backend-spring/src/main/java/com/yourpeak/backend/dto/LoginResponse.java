package com.yourpeak.backend.dto;

import java.time.LocalDate;

public record LoginResponse(
        Long idUsuario,
        String nombre,
        String apellido,
        String correo,
        String rol,
        String estado,
        Boolean membresiaVigente,
        PeriodoVigente membresia
) {
    public record PeriodoVigente(
            String nombrePlan, Byte duracionMeses, LocalDate fechaInicio, LocalDate fechaVencimiento
    ) { }
}
