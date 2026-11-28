package com.ramirez.yourpeak.ui

import com.ramirez.yourpeak.ui.components.claveIlustracion
import com.ramirez.yourpeak.ui.components.claveIlustracionPorNombre
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** La imagenRuta que entrega la API debe resolver al nombre del recurso incluido en la app. */
class IlustracionEjercicioTest {
    @Test
    fun usaElNombreParaLasIlustracionesIncluidas() {
        assertEquals("curl_de_biceps", claveIlustracionPorNombre("Curl de bíceps"))
        assertEquals("jalon_al_pecho", claveIlustracionPorNombre(" Jalón al pecho "))
        assertEquals("extension_de_triceps_en_polea", claveIlustracionPorNombre("Extensión de tríceps en polea"))
        assertNull(claveIlustracionPorNombre(null))
        assertNull(claveIlustracionPorNombre(" "))
    }

    @Test
    fun convierteLaRutaEnLaClaveDelRecurso() {
        assertEquals("jalon_al_pecho", claveIlustracion("jalon-al-pecho"))
        assertEquals("curl_de_biceps", claveIlustracion("ejercicios/Curl-De-Biceps.jpg"))
        assertEquals("aperturas_con_mancuernas", claveIlustracion(" aperturas-con-mancuernas "))
    }

    @Test
    fun rechazaRutasVaciasOInvalidas() {
        assertNull(claveIlustracion(null))
        assertNull(claveIlustracion(""))
        assertNull(claveIlustracion("jalón al pecho"))
    }
}
