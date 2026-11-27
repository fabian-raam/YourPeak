package com.ramirez.yourpeak.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ramirez.yourpeak.data.model.ApiErrorResponse
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.model.NuevaSolicitudAccesoRequest
import com.ramirez.yourpeak.data.model.SolicitudAccesoResponse
import com.ramirez.yourpeak.data.network.ApiService
import retrofit2.Response
import java.io.IOException

sealed interface ResultadoSolicitud<out T> {
    data class Exito<T>(val datos: T) : ResultadoSolicitud<T>
    data class Error(val mensaje: String, val codigo: Int? = null) : ResultadoSolicitud<Nothing>
}

/** Consulta el estado de la cuenta y de la solicitud de acceso, y permite volver a solicitarla. */
class SolicitudAccesoRepository(
    private val api: ApiService,
    private val gson: Gson,
    private val apiKey: String
) {
    suspend fun consultarUsuario(idUsuario: Long): ResultadoSolicitud<LoginResponse> =
        llamar("No se pudo consultar tu cuenta") { api.consultarUsuario(apiKey, idUsuario) }

    suspend fun consultar(idUsuario: Long): ResultadoSolicitud<SolicitudAccesoResponse> =
        llamar("No se pudo consultar tu solicitud") { api.consultarSolicitud(apiKey, idUsuario) }

    suspend fun volverASolicitar(idUsuario: Long, mensaje: String?): ResultadoSolicitud<SolicitudAccesoResponse> {
        val texto = mensaje?.trim()?.takeIf(String::isNotEmpty)?.take(300)
        return llamar("No se pudo enviar tu solicitud") {
            api.volverASolicitar(apiKey, idUsuario, NuevaSolicitudAccesoRequest(texto))
        }
    }

    private suspend fun <T> llamar(mensajeError: String, peticion: suspend () -> Response<T>): ResultadoSolicitud<T> {
        val respuesta = try {
            peticion()
        } catch (_: IOException) {
            return ResultadoSolicitud.Error("No se pudo conectar con el servidor.")
        } catch (_: RuntimeException) {
            return ResultadoSolicitud.Error("El servidor devolvió una respuesta inválida.")
        }
        if (respuesta.isSuccessful) {
            return respuesta.body()?.let { ResultadoSolicitud.Exito(it) }
                ?: ResultadoSolicitud.Error("El servidor devolvió una respuesta vacía.")
        }
        val detalle = try {
            respuesta.errorBody()?.string()?.let { gson.fromJson(it, ApiErrorResponse::class.java)?.detail }
        } catch (_: JsonParseException) {
            null
        } catch (_: IOException) {
            null
        }
        return ResultadoSolicitud.Error(
            detalle?.takeIf(String::isNotBlank) ?: "$mensajeError (${respuesta.code()}).",
            respuesta.code()
        )
    }
}
