package com.ramirez.yourpeak.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ramirez.yourpeak.data.model.ApiErrorResponse
import com.ramirez.yourpeak.data.model.RegistroProgresoRequest
import com.ramirez.yourpeak.data.model.RegistroProgresoResponse
import com.ramirez.yourpeak.data.network.ApiService
import retrofit2.Response
import java.io.IOException

sealed interface ResultadoProgreso<out T> {
    data class Exito<T>(val dato: T) : ResultadoProgreso<T>
    data class Error(val mensaje: String) : ResultadoProgreso<Nothing>
}

/** Aísla las llamadas de progreso para que la pantalla no dependa de Retrofit. */
class ProgresoRepository(
    private val api: ApiService,
    private val gson: Gson,
    private val apiKey: String
) {
    suspend fun consultar(idUsuario: Long): ResultadoProgreso<List<RegistroProgresoResponse>> =
        ejecutar { api.consultarProgreso(apiKey, idUsuario) }

    suspend fun registrar(
        idUsuario: Long,
        request: RegistroProgresoRequest
    ): ResultadoProgreso<RegistroProgresoResponse> =
        ejecutar { api.registrarPeso(apiKey, idUsuario, request) }

    private suspend fun <T : Any> ejecutar(
        llamada: suspend () -> Response<T>
    ): ResultadoProgreso<T> {
        val respuesta = try {
            llamada()
        } catch (_: IOException) {
            return ResultadoProgreso.Error("No se pudo conectar con el servidor.")
        } catch (_: RuntimeException) {
            return ResultadoProgreso.Error("El servidor devolvió una respuesta inválida.")
        }
        if (respuesta.isSuccessful) {
            return respuesta.body()?.let { ResultadoProgreso.Exito(it) }
                ?: ResultadoProgreso.Error("El servidor devolvió una respuesta vacía.")
        }
        return ResultadoProgreso.Error(mensajeError(respuesta))
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
                400 -> "Ingresa un peso válido, con hasta dos decimales."
                403 -> "Solo una cuenta cliente activa puede consultar su progreso."
                409 -> "Completa primero tu perfil para registrar tu peso."
                in 500..599 -> "No se pudo actualizar tu progreso. Inténtalo más tarde."
                else -> "Ocurrió un error al consultar el progreso (${respuesta.code()})."
            }
    }
}
