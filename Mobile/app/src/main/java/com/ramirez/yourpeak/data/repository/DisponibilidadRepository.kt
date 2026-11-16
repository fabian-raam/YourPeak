package com.ramirez.yourpeak.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ramirez.yourpeak.data.model.ApiErrorResponse
import com.ramirez.yourpeak.data.model.DisponibilidadRequest
import com.ramirez.yourpeak.data.model.DisponibilidadResponse
import com.ramirez.yourpeak.data.network.ApiService
import retrofit2.Response
import java.io.IOException

sealed interface ResultadoDisponibilidad {
    data class Exito(val disponibilidad: DisponibilidadResponse) : ResultadoDisponibilidad
    data class Error(val mensaje: String) : ResultadoDisponibilidad
}

/** Mantiene las llamadas HTTP de disponibilidad fuera de las pantallas. */
class DisponibilidadRepository(
    private val api: ApiService,
    private val gson: Gson,
    private val apiKey: String
) {
    suspend fun consultar(idUsuario: Long): ResultadoDisponibilidad = ejecutar {
        api.consultarDisponibilidad(apiKey, idUsuario)
    }

    suspend fun guardar(
        idUsuario: Long,
        request: DisponibilidadRequest
    ): ResultadoDisponibilidad = ejecutar {
        api.guardarDisponibilidad(apiKey, idUsuario, request)
    }

    private suspend fun ejecutar(
        llamada: suspend () -> Response<DisponibilidadResponse>
    ): ResultadoDisponibilidad {
        val respuesta = try {
            llamada()
        } catch (_: IOException) {
            return ResultadoDisponibilidad.Error("No se pudo conectar con el servidor.")
        } catch (_: RuntimeException) {
            return ResultadoDisponibilidad.Error("El servidor devolvió una respuesta inválida.")
        }

        if (respuesta.isSuccessful) {
            return respuesta.body()?.let { ResultadoDisponibilidad.Exito(it) }
                ?: ResultadoDisponibilidad.Error("El servidor devolvió una respuesta vacía.")
        }
        return ResultadoDisponibilidad.Error(mensajeError(respuesta))
    }

    private fun mensajeError(respuesta: Response<*>): String {
        val detalle = try {
            respuesta.errorBody()?.string()
                ?.let { gson.fromJson(it, ApiErrorResponse::class.java)?.detail }
        } catch (_: JsonParseException) {
            null
        } catch (_: IOException) {
            null
        }
        return detalle?.takeIf(String::isNotBlank)
            ?: when (respuesta.code()) {
                400 -> "Revisa que cada día disponible tenga un rango de horas válido."
                403 -> "Solo una cuenta activa puede gestionar su disponibilidad."
                in 500..599 -> "No se pudo guardar la disponibilidad. Inténtalo más tarde."
                else -> "Ocurrió un error al actualizar la disponibilidad (${respuesta.code()})."
            }
    }
}
