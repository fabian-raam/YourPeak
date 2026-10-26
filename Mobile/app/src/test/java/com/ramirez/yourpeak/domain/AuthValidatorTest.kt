package com.ramirez.yourpeak.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun aceptaCorreoInstitucionalConEspaciosYMayusculas() {
        assertNull(AuthValidator.validarCorreo("  Ana.Torres@TECSUP.edu.pe "))
    }

    @Test
    fun rechazaOtrosDominios() {
        assertEquals(
            "Usa tu correo institucional @tecsup.edu.pe.",
            AuthValidator.validarCorreo("ana@gmail.com")
        )
        assertTrue(AuthValidator.validarCorreo("ana@tecsup.edu.pe.com") != null)
    }

    @Test
    fun loginExigeContrasena() {
        val errores = AuthValidator.validarLogin("ana@tecsup.edu.pe", "")
        assertEquals(setOf("contrasena"), errores.keys)
    }

    @Test
    fun registroValidoNoTieneErrores() {
        val errores = AuthValidator.validarRegistro(
            "Ana", "Torres", "2000-05-10", "ana@tecsup.edu.pe", "frase segura", "frase segura", hoy = "2026-10-26"
        )
        assertTrue(errores.isEmpty())
    }

    @Test
    fun registroDetectaCadaCampoInvalido() {
        val errores = AuthValidator.validarRegistro(
            " ", "", "2026-10-26", "ana@gmail.com", "corta", "otra", hoy = "2026-10-26"
        )
        assertEquals(
            setOf("nombre", "apellido", "fechaNacimiento", "correo", "contrasena", "confirmacion"),
            errores.keys
        )
    }
}
