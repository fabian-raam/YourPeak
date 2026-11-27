package com.ramirez.yourpeak.ui

import com.ramirez.yourpeak.ui.components.claveIlustracion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** La imagenRuta que entrega la API debe resolver al nombre del recurso incluido en la app. */
class IlustracionEjercicioTest {
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
