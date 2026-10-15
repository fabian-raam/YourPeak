package com.frontendusuario.app.data.repository

import com.google.gson.Gson
import com.frontendusuario.app.data.remote.ApiClient
import com.frontendusuario.app.data.remote.ApiError
import com.frontendusuario.app.data.remote.RegistroApi
import com.frontendusuario.app.data.remote.RegistroRequest
import com.frontendusuario.app.data.remote.RegistroResponse
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Representa los posibles resultados de la operación de registro.
 */
sealed interface RegistroResult {
    data class Success(val response: RegistroResponse) : RegistroResult
    data class ValidationError(val detail: String?, val campos: Map<String, String>) : RegistroResult
    data class ConflictError(val detail: String?) : RegistroResult
    data class ConnectionError(val message: String) : RegistroResult
    data class TimeoutError(val message: String) : RegistroResult
    data class ServerError(val detail: String?) : RegistroResult
    data class UnknownError(val message: String) : RegistroResult
}

/**
 * Repositorio encargado de la gestión del registro de usuarios.
 */
open class RegistroRepository(
    private val api: RegistroApi = ApiClient.registroApi,
    private val gson: Gson = Gson(),
) {
    /**
     * Envía la solicitud de registro al servidor remoto.
     */
    open suspend fun registrar(request: RegistroRequest): RegistroResult {
        return try {
            val response = api.registrar(request)
            if (response.code() == 201) {
                val body = response.body()
                val hasValidUser = (body?.idUsuario != null) && (body.idUsuario > 0)
                val hasValidClient = (body?.idCliente != null) && (body.idCliente > 0)
                val isEstadoPendiente = body?.estado?.lowercase() == "pendiente"

                if (body != null && hasValidUser && hasValidClient && isEstadoPendiente) {
                    RegistroResult.Success(body)
                } else {
                    RegistroResult.UnknownError(
                        "La respuesta de registro no contiene un cuerpo válido, identificadores o estado pendiente.",
                    )
                }
            } else {
                val errorBodyString = response.errorBody()?.string()
                val apiError = parseApiError(errorBodyString)

                when (response.code()) {
                    400 -> RegistroResult.ValidationError(
                        detail = apiError?.detail ?: "Datos de registro no válidos.",
                        campos = apiError?.campos ?: emptyMap(),
                    )
                    409 -> RegistroResult.ConflictError(
                        detail = apiError?.detail ?: "El correo electrónico ya se encuentra registrado.",
                    )
                    500 -> RegistroResult.ServerError(
                        detail = apiError?.detail ?: "Ocurrió un error interno en el servidor.",
                    )
                    else -> RegistroResult.UnknownError(
                        apiError?.detail ?: "Respuesta inesperada del servidor (código ${response.code()}).",
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: SocketTimeoutException) {
            RegistroResult.TimeoutError(
                "No se pudo confirmar el resultado del registro debido a un tiempo de espera agotado. El servidor podría haber completado el registro.",
            )
        } catch (_: TimeoutException) {
            RegistroResult.TimeoutError(
                "No se pudo confirmar el resultado del registro debido a un tiempo de espera agotado. El servidor podría haber completado el registro.",
            )
        } catch (_: IOException) {
            RegistroResult.ConnectionError(
                "No se pudo conectar con el servidor. Verifica tu conexión a internet o el estado del backend.",
            )
        } catch (e: Exception) {
            RegistroResult.UnknownError(
                e.message ?: "Ocurrió un error inesperado durante el proceso de registro.",
            )
        }
    }

    private fun parseApiError(errorBodyJson: String?): ApiError? {
        if (errorBodyJson.isNullOrBlank()) return null
        return try {
            gson.fromJson(errorBodyJson, ApiError::class.java)
        } catch (_: Exception) {
            null
        }
    }
}
