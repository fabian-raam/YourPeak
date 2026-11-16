package com.ramirez.yourpeak.data.model

/** Respuestas y solicitudes equivalentes a los DTO de disponibilidad de Spring. */
data class DisponibilidadResponse(
    val dias: List<DiaDisponibilidadResponse>?
)

data class DiaDisponibilidadResponse(
    val diaSemana: Int?,
    val disponible: Boolean?,
    val horaInicio: String?,
    val horaFin: String?
)

data class DisponibilidadRequest(
    val dias: List<DiaDisponibilidadRequest>
)

data class DiaDisponibilidadRequest(
    val diaSemana: Int,
    val disponible: Boolean,
    val horaInicio: String?,
    val horaFin: String?
)
