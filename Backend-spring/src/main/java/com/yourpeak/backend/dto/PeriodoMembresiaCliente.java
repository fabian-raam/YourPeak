package com.yourpeak.backend.dto;

import java.time.LocalDate;

public record PeriodoMembresiaCliente(
        Long idMembresia, String nombrePlan, Byte duracionMeses,
        LocalDate fechaInicio, LocalDate fechaVencimiento, String estadoPeriodo
) { }
