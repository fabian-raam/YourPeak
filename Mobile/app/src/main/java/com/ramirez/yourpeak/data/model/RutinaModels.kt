package com.ramirez.yourpeak.data.model

/** Modelos alineados con los DTO de rutina semanal que expone Spring. */
data class RutinaResponse(
    val dias: List<DiaRutinaResponse>?
)

data class DiaRutinaResponse(
    val diaSemana: Int?,
    val disponible: Boolean?,
    val horaInicio: String?,
    val horaFin: String?,
    val ejercicios: List<EjercicioRutinaResponse>?
)

data class EjercicioRutinaResponse(
    val idEjercicio: Int?,
    val nombre: String?,
    val grupoMuscular: String?,
    val tipoMedicion: String?,
    val orden: Int?,
    val series: Int?,
    val repeticiones: Int?,
    val segundos: Int?,
    val descansoSegundos: Int?,
    val origen: String?
)

data class RutinaRequest(
    val dias: List<DiaRutinaRequest>
)

data class DiaRutinaRequest(
    val diaSemana: Int,
    val ejercicios: List<EjercicioRutinaRequest>
)

data class EjercicioRutinaRequest(
    val idEjercicio: Int,
    val series: Int,
    val repeticiones: Int?,
    val segundos: Int?,
    val descansoSegundos: Int,
    val origen: String? = null
)
