package com.ramirez.yourpeak.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ramirez.yourpeak.data.model.ApiErrorResponse
import com.ramirez.yourpeak.data.model.LoginRequest
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.model.RegistroRequest
import com.ramirez.yourpeak.data.model.RegistroResponse
import com.ramirez.yourpeak.data.network.ApiService
import retrofit2.Response
import java.io.IOException
import java.util.Locale

/** Resultado de una llamada a Spring, con mensajes listos para mostrar en pantalla. */
sealed interface ApiResult<out T> {
    data class Exito<T>(val datos: T) : ApiResult<T>
    data class Error(
        val mensaje: String,
        val campos: Map<String, String> = emptyMap()
    ) : ApiResult<Nothing>
}

class AuthRepository(
    private val api: ApiService,
    private val gson: Gson
) {

    suspend fun login(correo: String, contrasena: String): ApiResult<LoginResponse> {
        val resultado = llamar {
            api.login(LoginRequest(normalizarCorreo(correo), contrasena))
        }
        if (resultado is ApiResult.Exito && resultado.datos.idUsuario == null) {
            return ApiResult.Error("El servidor devolvió una respuesta inválida.")
        }
        return resultado
    }

    suspend fun registrar(
        nombre: String,
        apellido: String,
        fechaNacimiento: String,
        correo: String,
        contrasena: String
    ): ApiResult<RegistroResponse> = llamar {
        api.registrar(
            RegistroRequest(
                nombre = nombre.trim(),
                apellido = apellido.trim(),
                fechaNacimiento = fechaNacimiento,
                correo = normalizarCorreo(correo),
                contrasena = contrasena
            )
        )
    }

    private suspend fun <T> llamar(peticion: suspend () -> Response<T>): ApiResult<T> {
        val respuesta = try {
            peticion()
        } catch (e: IOException) {
            Log.e("AuthRepository", "Error de red al comunicarse con la API: ${e.message}", e)
            return ApiResult.Error(
                "No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo."
            )
        } catch (e: RuntimeException) {
            Log.e("AuthRepository", "Error inesperado en respuesta del servidor", e)
            return ApiResult.Error("El servidor devolvió una respuesta inválida.")
        }

        val cuerpo = respuesta.body()
        if (respuesta.isSuccessful && cuerpo != null) {
            return ApiResult.Exito(cuerpo)
        }
        return errorDesde(respuesta)
    }

    private fun errorDesde(respuesta: Response<*>): ApiResult.Error {
        val error = try {
            respuesta.errorBody()?.string()?.let { gson.fromJson(it, ApiErrorResponse::class.java) }
        } catch (_: JsonParseException) {
            null
        } catch (_: IOException) {
            null
        }

        val mensaje = error?.detail?.takeIf { it.isNotBlank() } ?: when (respuesta.code()) {
            400 -> "Revisa los datos ingresados."
            401 -> "Correo o contraseña incorrectos."
            409 -> "No se puede completar el registro con ese correo."
            in 500..599 -> "El servicio no está disponible. Inténtalo más tarde."
            else -> "Ocurrió un error inesperado (${respuesta.code()})."
        }
        return ApiResult.Error(mensaje, error?.campos.orEmpty())
    }

    private fun normalizarCorreo(correo: String) = correo.trim().lowercase(Locale.ROOT)
}
