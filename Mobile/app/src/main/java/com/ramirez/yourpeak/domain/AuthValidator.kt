package com.ramirez.yourpeak.domain

import java.util.Locale

/** Mismas reglas que valida Spring Boot (LoginRequest / RegistroRequest). */
object AuthValidator {

    private val correoInstitucional = Regex("^[^@\\s]+@tecsup\\.edu\\.pe$")

    fun validarCorreo(correo: String): String? {
        val valor = correo.trim().lowercase(Locale.ROOT)
        return when {
            valor.isEmpty() -> "Ingresa tu correo institucional."
            valor.length > 254 -> "El correo admite hasta 254 caracteres."
            !correoInstitucional.matches(valor) -> "Usa tu correo institucional @tecsup.edu.pe."
            else -> null
        }
    }

    fun validarLogin(correo: String, contrasena: String): Map<String, String> = buildMap {
        validarCorreo(correo)?.let { put("correo", it) }
        when {
            contrasena.isEmpty() -> put("contrasena", "Ingresa tu contraseña.")
            contrasena.length > 128 -> put("contrasena", "La contraseña admite hasta 128 caracteres.")
        }
    }

    /**
     * @param fechaNacimiento fecha en formato yyyy-MM-dd
     * @param hoy fecha actual en formato yyyy-MM-dd (las fechas ISO se comparan como texto)
     */
    fun validarRegistro(
        nombre: String,
        apellido: String,
        fechaNacimiento: String,
        correo: String,
        contrasena: String,
        confirmacion: String,
        hoy: String
    ): Map<String, String> = buildMap {
        validarNombre(nombre, "nombre")?.let { put("nombre", it) }
        validarNombre(apellido, "apellido")?.let { put("apellido", it) }
        if (fechaNacimiento.isBlank() || fechaNacimiento >= hoy) {
            put("fechaNacimiento", "Ingresa una fecha anterior a hoy.")
        }
        validarCorreo(correo)?.let { put("correo", it) }
        if (contrasena.length !in 8..128) {
            put("contrasena", "La contraseña debe tener entre 8 y 128 caracteres.")
        }
        if (confirmacion != contrasena) {
            put("confirmacion", "Las contraseñas no coinciden.")
        }
    }

    private fun validarNombre(valor: String, campo: String): String? {
        val limpio = valor.trim()
        return when {
            limpio.isEmpty() -> "Ingresa tu $campo."
            limpio.length > 100 -> "El $campo admite hasta 100 caracteres."
            else -> null
        }
    }
}
