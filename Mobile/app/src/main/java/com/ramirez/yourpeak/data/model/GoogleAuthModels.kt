package com.ramirez.yourpeak.data.model

data class GoogleChallenge(val clientId: String, val nonce: String)

data class GoogleTokenRequest(val nonce: String, val credential: String) {
    override fun toString() = "GoogleTokenRequest[datos ocultos]"
}

data class GoogleCompleteRequest(
    val flowId: String,
    val nombre: String? = null,
    val apellido: String? = null,
    val fechaNacimiento: String? = null,
    val contrasena: String? = null
) {
    override fun toString() = "GoogleCompleteRequest[datos ocultos]"
}

data class GoogleProfile(val nombre: String, val apellido: String, val correo: String)

data class GoogleAuthResponse(
    val estado: String,
    val usuario: LoginResponse?,
    val flowId: String?,
    val perfil: GoogleProfile?
)
