package com.ramirez.yourpeak.data.model

/** Modelos alineados con los DTO de rutina semanal que expone Spring. */
data class RutinaResponse(
    val dias: List<DiaRutinaResponse>?,
    val actividadReciente: List<ActividadRutinaResponse>? = null
)

data class ActividadRutinaResponse(
    val fecha: String?,
    val titulo: String?,
    val ejercicios: Int?
)

data class DiaRutinaResponse(
    val diaSemana: Int?,
    val disponible: Boolean?,
    val horaInicio: String?,
    val horaFin: String?,
    val ejercicios: List<EjercicioRutinaResponse>?
)

data class EjercicioRutinaResponse(
    val idPlanEjercicio: Long?,
    val idEjercicio: Int?,
    val nombre: String?,
    val grupoMuscular: String?,
    val tipoMedicion: String?,
    val orden: Int?,
    val series: Int?,
    val repeticiones: Int?,
    val segundos: Int?,
    val descansoSegundos: Int?,
    val origen: String?,
    val imagenRuta: String? = null
)

/** Propuesta de rutina generada por Spring (Gemini o reglas); no se guarda hasta llamar a guardar. */
data class RutinaGeneradaResponse(
    val fuente: String?,
    val mensaje: String?,
    val dias: List<DiaRutinaResponse>?
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

data class RegistrarEjercicioRealizadoRequest(
    val series: List<SerieEjercicioRealizadoRequest>
)

data class SerieEjercicioRealizadoRequest(
    val numeroSerie: Int,
    val repeticiones: Int? = null,
    val segundos: Int? = null
)

data class EjercicioRealizadoResponse(
    val idPlanEjercicio: Long?,
    val ejercicio: String?,
    val fecha: String?,
    val seriesCompletadas: Int?,
    val completado: Boolean?,
    val detail: String?
)
