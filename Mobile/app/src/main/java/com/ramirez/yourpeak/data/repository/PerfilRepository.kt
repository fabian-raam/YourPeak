package com.ramirez.yourpeak.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ramirez.yourpeak.data.model.ApiErrorResponse
import com.ramirez.yourpeak.data.model.PerfilInicialRequest
import com.ramirez.yourpeak.data.model.PerfilInicialResponse
import com.ramirez.yourpeak.data.network.ApiService
import retrofit2.Response
import java.io.IOException

sealed interface ResultadoPerfil {
    data class Exito(val perfil: PerfilInicialResponse) : ResultadoPerfil
    data class Error(val mensaje: String) : ResultadoPerfil
}

/** Aísla las llamadas del perfil inicial para que la pantalla no dependa de Retrofit. */
class PerfilRepository(
    private val api: ApiService,
    private val gson: Gson,
    private val apiKey: String
) {
    suspend fun consultar(idUsuario: Long): ResultadoPerfil = ejecutar {
        api.consultarPerfil(apiKey, idUsuario)
    }

    suspend fun guardar(idUsuario: Long, request: PerfilInicialRequest): ResultadoPerfil = ejecutar {
        api.guardarPerfil(apiKey, idUsuario, request)
    }

    private suspend fun ejecutar(llamada: suspend () -> Response<PerfilInicialResponse>): ResultadoPerfil {
        val respuesta = try {
            llamada()
        } catch (_: IOException) {
            return ResultadoPerfil.Error("No se pudo conectar con el servidor.")
        } catch (_: RuntimeException) {
            return ResultadoPerfil.Error("El servidor devolvió una respuesta inválida.")
        }
        if (respuesta.isSuccessful) {
            return respuesta.body()?.let { ResultadoPerfil.Exito(it) }
                ?: ResultadoPerfil.Error("El servidor devolvió una respuesta vacía.")
        }
        return ResultadoPerfil.Error(mensajeError(respuesta))
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
                400 -> "Revisa los datos de tu perfil."
                403 -> "Solo una cuenta cliente activa puede gestionar su perfil."
                409 -> "No se pudo guardar tu perfil con esos datos."
                in 500..599 -> "No se pudo guardar tu perfil. Inténtalo más tarde."
                else -> "Ocurrió un error al consultar tu perfil (${respuesta.code()})."
            }
    }
}
