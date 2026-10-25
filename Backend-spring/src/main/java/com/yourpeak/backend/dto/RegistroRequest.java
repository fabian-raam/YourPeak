package com.yourpeak.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

public record RegistroRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido admite hasta 100 caracteres")
        String apellido,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato válido")
        @Size(max = 254, message = "El correo admite hasta 254 caracteres")
        @Pattern(regexp = "^[^@\\s]+@tecsup\\.edu\\.pe$", message = "Usa tu correo institucional @tecsup.edu.pe")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(
                min = 15,
                max = 128,
                message = "La contraseña debe tener entre 15 y 128 caracteres"
        )
        String contrasena

) {

    public RegistroRequest {
        nombre = nombre == null ? null : nombre.strip();
        apellido = apellido == null ? null : apellido.strip();
        correo = correo == null
                ? null
                : correo.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "RegistroRequest[datos personales y contraseña ocultos]";
    }
}