package com.ramirez.yourpeak.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ramirez.yourpeak.data.model.ApiErrorResponse
import com.ramirez.yourpeak.data.model.EjercicioResponse
import com.ramirez.yourpeak.data.network.ApiService
import retrofit2.Response
import java.io.IOException

sealed interface ResultadoEjercicios {
    data class Exito(val ejercicios: List<EjercicioResponse>) : ResultadoEjercicios
    data class Error(val mensaje: String) : ResultadoEjercicios
}

/** Encapsula la consulta del catálogo para que la UI no conozca Retrofit. */
class EjercicioRepository(
    private val api: ApiService,
    private val gson: Gson,
    private val apiKey: String
) {
    suspend fun listarDisponibles(idUsuario: Long): ResultadoEjercicios {
        val respuesta = try {
            api.listarEjerciciosDisponibles(apiKey, idUsuario)
        } catch (_: IOException) {
            return ResultadoEjercicios.Error("No se pudo conectar con el servidor.")
        } catch (_: RuntimeException) {
            return ResultadoEjercicios.Error("El servidor devolvió una respuesta inválida.")
        }

        if (respuesta.isSuccessful) {
            return respuesta.body()?.let { ResultadoEjercicios.Exito(it) }
                ?: ResultadoEjercicios.Error("El servidor devolvió una lista vacía inválida.")
        }
        return ResultadoEjercicios.Error(mensajeError(respuesta))
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
                403 -> "Tu cuenta no tiene acceso al catálogo de ejercicios."
                in 500..599 -> "El catálogo no está disponible por ahora."
                else -> "No se pudieron cargar los ejercicios (${respuesta.code()})."
            }
    }
}
