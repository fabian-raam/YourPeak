package com.ramirez.yourpeak.ui

import com.ramirez.yourpeak.ui.components.claveIlustracion
import org.junit.Assert.assertEquals
import org.junit.Test

/** Los nombres del catálogo, con tildes y mayúsculas, deben encontrar su ilustración. */
class IlustracionEjercicioTest {
    @Test
    fun normalizaTildesEspaciosYMayusculas() {
        assertEquals("jalon_al_pecho", claveIlustracion("Jalón al pecho"))
        assertEquals("curl_de_biceps", claveIlustracion(" Curl de Bíceps "))
        assertEquals("extension_de_triceps_en_polea", claveIlustracion("Extensión de tríceps en polea"))
        assertEquals("", claveIlustracion(null))
    }
}
