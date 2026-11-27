package com.ramirez.yourpeak.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ramirez.yourpeak.data.model.ApiErrorResponse
import com.ramirez.yourpeak.data.model.AsistenteRequest
import com.ramirez.yourpeak.data.model.AsistenteResponse
import com.ramirez.yourpeak.data.model.MensajeAsistente
import com.ramirez.yourpeak.data.network.ApiService
import java.io.IOException

sealed interface ResultadoAsistente {
    data class Exito(val respuesta: AsistenteResponse) : ResultadoAsistente
    data class Error(val mensaje: String) : ResultadoAsistente
}

/** Envía preguntas al asistente de alimentación y ejercicio (Spring + Gemini). */
class AsistenteRepository(
    private val api: ApiService,
    private val gson: Gson,
    private val apiKey: String
) {
    suspend fun preguntar(idUsuario: Long, pregunta: String, historial: List<MensajeAsistente>): ResultadoAsistente {
        val solicitud = AsistenteRequest(pregunta.trim().take(500), historial.takeLast(12))
        val respuesta = try {
            api.preguntarAsistente(apiKey, idUsuario, solicitud)
        } catch (_: IOException) {
            return ResultadoAsistente.Error("No se pudo conectar con el servidor.")
        } catch (_: RuntimeException) {
            return ResultadoAsistente.Error("El servidor devolvió una respuesta inválida.")
        }
        if (respuesta.isSuccessful) {
            val cuerpo = respuesta.body()
            return if (cuerpo?.respuesta.isNullOrBlank()) ResultadoAsistente.Error("El asistente no envió una respuesta.")
            else ResultadoAsistente.Exito(cuerpo!!)
        }
        val detalle = try {
            respuesta.errorBody()?.string()?.let { gson.fromJson(it, ApiErrorResponse::class.java)?.detail }
        } catch (_: JsonParseException) {
            null
        } catch (_: IOException) {
            null
        }
        return ResultadoAsistente.Error(
            detalle?.takeIf(String::isNotBlank) ?: "El asistente no pudo responder (${respuesta.code()})."
        )
    }
}
