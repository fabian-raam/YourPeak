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
    val estado: String?,
    val membresiaVigente: Boolean? = null
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

/** Periodo de membresía del cliente (GET /api/cliente/perfil/membresias). */
data class MembresiaPeriodoResponse(
    val idMembresia: Long?,
    val idPlanMembresia: Int?,
    val nombrePlan: String?,
    val fechaInicio: String?,
    val fechaVencimiento: String?,
    val estadoPeriodo: String?,
    val observacion: String?
)

/** Estado de la solicitud de acceso del cliente y su historial (más reciente primero). */
data class SolicitudAccesoResponse(
    val estado: String?,
    val puedeSolicitar: Boolean = false,
    val solicitudesRestantesHoy: Int = 0,
    val limiteDiario: Int = 0,
    val historial: List<SolicitudAccesoItem>? = null
)

data class SolicitudAccesoItem(
    val numero: Int,
    val estado: String?,
    val mensajeCliente: String?,
    val motivoRechazo: String?,
    val fechaSolicitud: String?,
    val fechaResolucion: String?
)

/** Mensaje opcional al volver a pedir acceso tras un rechazo. */
data class NuevaSolicitudAccesoRequest(val mensaje: String?)
