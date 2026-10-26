package com.ramirez.yourpeak.data.model

// Modelos que replican los DTO de Spring Boot (paquete com.yourpeak.backend.dto).
// Los campos de respuesta son anulables porque Gson no respeta la nulabilidad de Kotlin.

data class LoginRequest(
    val correo: String,
    val contrasena: String
)

data class LoginResponse(
    val idUsuario: Long?,
    val nombre: String?,
    val apellido: String?,
    val correo: String?,
    val rol: String?,
    val estado: String?
)

data class RegistroRequest(
    val nombre: String,
    val apellido: String,
    /** Formato ISO yyyy-MM-dd, igual que LocalDate en Spring. */
    val fechaNacimiento: String,
    val correo: String,
    val contrasena: String
)

data class RegistroResponse(
    val idUsuario: Long?,
    val idCliente: Long?,
    val nombre: String?,
    val apellido: String?,
    val correo: String?,
    val estado: String?,
    val mensaje: String?
)

/** Cuerpo de error de Spring: ProblemDetail ({title, detail, campos}) o {detail}. */
data class ApiErrorResponse(
    val title: String?,
    val detail: String?,
    val campos: Map<String, String>?
)
