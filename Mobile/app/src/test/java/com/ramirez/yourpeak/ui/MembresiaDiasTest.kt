package com.ramirez.yourpeak.ui

import com.ramirez.yourpeak.data.model.MembresiaPeriodoResponse
import com.ramirez.yourpeak.ui.home.MiMembresiaUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** El anillo de Mi membresía cuenta los días que faltan, incluido hoy, sobre el total del periodo. */
class MembresiaDiasTest {
    private fun estado(vararg periodos: MembresiaPeriodoResponse) =
        MiMembresiaUiState(estadoCuenta = "ACTIVO", membresiaVigente = true, periodos = periodos.toList())

    private fun periodo(inicio: String, fin: String, estado: String) =
        MembresiaPeriodoResponse(1, 1, "Mensual", inicio, fin, estado, null)

    @Test
    fun cuentaLosDiasRestantesDelPeriodoVigente() {
        val dias = estado(periodo("2026-11-22", "2026-12-21", "VIGENTE")).diasRestantes(LocalDate.parse("2026-11-25"))
        assertEquals(27 to 30, dias)
    }

    @Test
    fun ignoraPeriodosVencidos() {
        assertNull(estado(periodo("2026-10-22", "2026-11-21", "VENCIDA")).diasRestantes(LocalDate.parse("2026-11-25")))
    }

    @Test
    fun unPeriodoProgramadoMuestraSuDuracionCompleta() {
        val dias = estado(periodo("2026-12-22", "2027-01-21", "PROGRAMADA")).diasRestantes(LocalDate.parse("2026-11-25"))
        assertEquals(31 to 31, dias)
    }
}
