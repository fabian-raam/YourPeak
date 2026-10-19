package com.frontendusuario.app.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Modelo de solicitud para el registro de usuarios.
 * Se implementa como una clase normal (no data class) para evitar que
 * el método toString() por defecto exponga la contraseña.
 */
class RegistroRequest(
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("apellido")
    val apellido: String,
    @SerializedName("correo")
    val correo: String,
    @SerializedName("contrasena")
    val contrasena: String,
) {
    override fun toString(): String {
        return "RegistroRequest[datos personales y contraseña ocultos]"
    }
}

/**
 * Modelo de respuesta tras un registro exitoso (HTTP 201).
 */
data class RegistroResponse(
    @SerializedName("idUsuario")
    val idUsuario: Long? = null,
    @SerializedName("idCliente")
    val idCliente: Long? = null,
    @SerializedName("nombre")
    val nombre: String? = null,
    @SerializedName("apellido")
    val apellido: String? = null,
    @SerializedName("correo")
    val correo: String? = null,
    @SerializedName("estado")
    val estado: String? = null,
    @SerializedName("mensaje")
    val mensaje: String? = null,
)

/**
 * Modelo para la representación de errores devueltos por la API (HTTP 400, 409, 500).
 */
data class ApiError(
    @SerializedName("detail")
    val detail: String? = null,
    @SerializedName("campos")
    val campos: Map<String, String>? = null,
)
