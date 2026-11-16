package com.ramirez.yourpeak.data.model

/** Representa el DTO EjercicioResponse que entrega Spring al cliente. */
data class EjercicioResponse(
    val idEjercicio: Int?,
    val idGrupoMuscular: Int?,
    val grupoMuscular: String?,
    val idNivelExperiencia: Int?,
    val nivelExperiencia: String?,
    val idTipoMedicion: Short?,
    val tipoMedicion: String?,
    val nombre: String?,
    val descripcion: String?,
    val equipamiento: String?,
    val imagenRuta: String?,
    val estado: String?
)
