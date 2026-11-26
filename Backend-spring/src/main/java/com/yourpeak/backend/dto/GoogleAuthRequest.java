package com.yourpeak.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GoogleAuthRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,255}") String subject,
        @NotBlank @Email @Size(max = 254) @Pattern(regexp = "^[^@\\s]+@tecsup\\.edu\\.pe$") String correo,
        @Size(max = 100) String nombre,
        @Size(max = 100) String apellido,
        @Past LocalDate fechaNacimiento,
        @Size(max = 128) String contrasena
) {
    @Override
    public String toString() {
        return "GoogleAuthRequest[datos ocultos]";
    }
}
