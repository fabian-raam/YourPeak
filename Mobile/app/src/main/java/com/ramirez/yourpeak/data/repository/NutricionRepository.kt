package com.ramirez.yourpeak.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ramirez.yourpeak.data.model.ApiErrorResponse
import com.ramirez.yourpeak.data.model.AlimentoNutricionResponse
import com.ramirez.yourpeak.data.model.IngestaNutricionResponse
import com.ramirez.yourpeak.data.model.RegistrarIngestaRequest
import com.ramirez.yourpeak.data.model.ResumenNutricionalResponse
import com.ramirez.yourpeak.data.network.ApiService
import retrofit2.Response
import java.io.IOException

sealed interface ResultadoNutricion<out T> {
    data class Exito<T>(val dato: T) : ResultadoNutricion<T>
    data class Error(val mensaje: String) : ResultadoNutricion<Nothing>
}

/** Centraliza las llamadas del registro de nutrición para mantener la UI libre de Retrofit. */
class NutricionRepository(
    private val api: ApiService,
    private val gson: Gson,
    private val apiKey: String
) {
    suspend fun listarAlimentos(idUsuario: Long) = ejecutar {
        api.listarAlimentosNutricion(apiKey, idUsuario)
    }

    suspend fun consultarDia(idUsuario: Long, fecha: String?) = ejecutar {
        api.consultarNutricionDia(apiKey, idUsuario, fecha)
    }

    suspend fun registrar(idUsuario: Long, request: RegistrarIngestaRequest) = ejecutar {
        api.registrarIngesta(apiKey, idUsuario, request)
    }

    suspend fun actualizar(
        idUsuario: Long,
        idIngesta: Long,
        request: RegistrarIngestaRequest
    ) = ejecutar {
        api.actualizarIngesta(apiKey, idUsuario, idIngesta, request)
    }

    suspend fun eliminar(idUsuario: Long, idIngesta: Long): ResultadoNutricion<Unit> {
        val respuesta = try {
            api.eliminarIngesta(apiKey, idUsuario, idIngesta)
        } catch (_: IOException) {
            return ResultadoNutricion.Error("No se pudo conectar con el servidor.")
        } catch (_: RuntimeException) {
            return ResultadoNutricion.Error("El servidor devolvió una respuesta inválida.")
        }
        return if (respuesta.isSuccessful) ResultadoNutricion.Exito(Unit)
        else ResultadoNutricion.Error(mensajeError(respuesta))
    }

    private suspend fun <T : Any> ejecutar(
        llamada: suspend () -> Response<T>
    ): ResultadoNutricion<T> {
        val respuesta = try {
            llamada()
        } catch (_: IOException) {
            return ResultadoNutricion.Error("No se pudo conectar con el servidor.")
        } catch (_: RuntimeException) {
            return ResultadoNutricion.Error("El servidor devolvió una respuesta inválida.")
        }
        if (!respuesta.isSuccessful) return ResultadoNutricion.Error(mensajeError(respuesta))
        return respuesta.body()?.let { ResultadoNutricion.Exito(it) }
            ?: ResultadoNutricion.Error("El servidor devolvió una respuesta vacía.")
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
                400 -> "Revisa el alimento y la cantidad en gramos."
                403 -> "Solo una cuenta cliente activa puede usar nutrición."
                404 -> "No se encontró ese alimento o consumo."
                in 500..599 -> "Nutrición no está disponible por ahora."
                else -> "Ocurrió un error al guardar el consumo (${respuesta.code()})."
            }
    }
}
