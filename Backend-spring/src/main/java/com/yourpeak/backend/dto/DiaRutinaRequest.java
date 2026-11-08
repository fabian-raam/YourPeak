package com.yourpeak.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DiaRutinaRequest(
        @NotNull(message = "Indica el día de la semana.")
        Integer diaSemana,

        @NotNull(message = "Envía la lista de ejercicios del día; puede estar vacía.")
        @Size(max = 100, message = "Un día no puede tener más de 100 ejercicios.")
        List<@NotNull @Valid EjercicioRutinaRequest> ejercicios
) { }
