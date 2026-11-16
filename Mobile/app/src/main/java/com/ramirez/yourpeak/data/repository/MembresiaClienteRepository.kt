package com.ramirez.yourpeak.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ramirez.yourpeak.data.model.ApiErrorResponse
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.network.ApiService
import java.io.IOException

sealed interface ResultadoMembresiaCliente {
    data class Exito(val usuario: LoginResponse) : ResultadoMembresiaCliente
    data class Error(val mensaje: String) : ResultadoMembresiaCliente
}

/** Actualiza el estado de membresía desde la respuesta vigente de Spring. */
class MembresiaClienteRepository(
    private val api: ApiService,
    private val gson: Gson,
    private val apiKey: String
) {
    suspend fun consultar(idUsuario: Long): ResultadoMembresiaCliente {
        val respuesta = try {
            api.consultarUsuario(apiKey, idUsuario)
        } catch (_: IOException) {
            return ResultadoMembresiaCliente.Error("No se pudo conectar con el servidor.")
        } catch (_: RuntimeException) {
            return ResultadoMembresiaCliente.Error("El servidor devolvió una respuesta inválida.")
        }

        if (respuesta.isSuccessful) {
            return respuesta.body()?.let { ResultadoMembresiaCliente.Exito(it) }
                ?: ResultadoMembresiaCliente.Error("El servidor devolvió una respuesta vacía.")
        }
        val detalle = try {
            respuesta.errorBody()?.string()
                ?.let { gson.fromJson(it, ApiErrorResponse::class.java)?.detail }
        } catch (_: JsonParseException) {
            null
        } catch (_: IOException) {
            null
        }
        return ResultadoMembresiaCliente.Error(
            detalle?.takeIf(String::isNotBlank)
                ?: "No se pudo actualizar el estado de la membresía (${respuesta.code()})."
        )
    }
}
