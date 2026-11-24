package com.ramirez.yourpeak.ui

import com.ramirez.yourpeak.ui.home.PerfilViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/** Las medidas del perfil siguen las reglas de Spring: de 0.01 a 999.99 con hasta dos decimales. */
class MedidaPerfilTest {
    @Test
    fun aceptaMedidasValidasConComaOPunto() {
        assertEquals(BigDecimal("70.50"), PerfilViewModel.medida("70,5"))
        assertEquals(BigDecimal("175.00"), PerfilViewModel.medida("175"))
        assertEquals(BigDecimal("999.99"), PerfilViewModel.medida("999.99"))
    }

    @Test
    fun rechazaMedidasFueraDeRangoOConMasDecimales() {
        assertNull(PerfilViewModel.medida(""))
        assertNull(PerfilViewModel.medida("0"))
        assertNull(PerfilViewModel.medida("1000"))
        assertNull(PerfilViewModel.medida("70.555"))
        assertNull(PerfilViewModel.medida("1.2.3"))
    }
}
