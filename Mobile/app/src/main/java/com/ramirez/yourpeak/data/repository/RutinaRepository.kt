package com.ramirez.yourpeak.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ramirez.yourpeak.data.model.ApiErrorResponse
import com.ramirez.yourpeak.data.model.RutinaGeneradaResponse
import com.ramirez.yourpeak.data.model.RutinaRequest
import com.ramirez.yourpeak.data.model.RutinaResponse
import com.ramirez.yourpeak.data.network.ApiService
import retrofit2.Response
import java.io.IOException

sealed interface ResultadoRutina {
    data class Exito(val rutina: RutinaResponse) : ResultadoRutina
    data class Error(val mensaje: String) : ResultadoRutina
}

sealed interface ResultadoGeneracion {
    data class Exito(val propuesta: RutinaGeneradaResponse) : ResultadoGeneracion
    data class Error(val mensaje: String) : ResultadoGeneracion
}

/** Aísla del ViewModel las consultas y actualizaciones de la rutina. */
class RutinaRepository(
    private val api: ApiService,
    private val gson: Gson,
    private val apiKey: String
) {
    suspend fun consultar(idUsuario: Long): ResultadoRutina = ejecutar {
        api.consultarRutina(apiKey, idUsuario)
    }

    suspend fun guardar(idUsuario: Long, request: RutinaRequest): ResultadoRutina = ejecutar {
        api.guardarRutina(apiKey, idUsuario, request)
    }

    /** Pide una propuesta con IA; Spring usa Gemini o, si no está disponible, el generador por reglas. */
    suspend fun generar(idUsuario: Long): ResultadoGeneracion {
        val respuesta = try {
            api.generarRutina(apiKey, idUsuario)
        } catch (_: IOException) {
            return ResultadoGeneracion.Error("No se pudo conectar con el servidor.")
        } catch (_: RuntimeException) {
            return ResultadoGeneracion.Error("El servidor devolvió una respuesta inválida.")
        }
        if (respuesta.isSuccessful) {
            return respuesta.body()?.takeIf { it.dias != null }?.let { ResultadoGeneracion.Exito(it) }
                ?: ResultadoGeneracion.Error("No se pudo interpretar la rutina propuesta.")
        }
        return ResultadoGeneracion.Error(mensajeError(respuesta))
    }

    private suspend fun ejecutar(llamada: suspend () -> Response<RutinaResponse>): ResultadoRutina {
        val respuesta = try {
            llamada()
        } catch (_: IOException) {
            return ResultadoRutina.Error("No se pudo conectar con el servidor.")
        } catch (_: RuntimeException) {
            return ResultadoRutina.Error("El servidor devolvió una respuesta inválida.")
        }

        if (respuesta.isSuccessful) {
            return respuesta.body()?.let { ResultadoRutina.Exito(it) }
                ?: ResultadoRutina.Error("El servidor devolvió una respuesta vacía.")
        }
        return ResultadoRutina.Error(mensajeError(respuesta))
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
                400 -> "Revisa los ejercicios y los días disponibles de la rutina."
                403 -> "Solo una cuenta activa puede gestionar su rutina."
                in 500..599 -> "No se pudo consultar la rutina. Inténtalo más tarde."
                else -> "Ocurrió un error al actualizar la rutina (${respuesta.code()})."
            }
    }
}
