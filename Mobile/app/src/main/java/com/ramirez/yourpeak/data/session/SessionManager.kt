package com.ramirez.yourpeak.data.session

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ramirez.yourpeak.data.model.LoginResponse

/**
 * Conserva el usuario que devolvió Spring tras el login.
 * Solo guarda datos de perfil; la contraseña nunca se almacena.
 */
class SessionManager(context: Context, private val gson: Gson) {

    private val preferencias =
        context.getSharedPreferences("yourpeak_sesion", Context.MODE_PRIVATE)

    fun guardar(usuario: LoginResponse) {
        preferencias.edit { putString(CLAVE_USUARIO, gson.toJson(usuario)) }
    }

    fun usuarioActual(): LoginResponse? {
        val json = preferencias.getString(CLAVE_USUARIO, null) ?: return null
        return try {
            gson.fromJson(json, LoginResponse::class.java)?.takeIf { it.idUsuario != null }
        } catch (_: JsonParseException) {
            null
        }
    }

    fun cerrarSesion() {
        preferencias.edit { remove(CLAVE_USUARIO) }
    }

    private companion object {
        const val CLAVE_USUARIO = "usuario"
    }
}
