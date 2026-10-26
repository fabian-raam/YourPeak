package com.ramirez.yourpeak.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Utilidades de fecha compatibles con minSdk 24 (java.time requiere API 26). */
object Fechas {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun formato(patron: String) =
        SimpleDateFormat(patron, Locale.ROOT).apply { timeZone = utc }

    /** Fecha local de hoy en formato ISO yyyy-MM-dd. */
    fun hoyIso(): String {
        val hoy = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            Locale.ROOT,
            hoy.get(Calendar.YEAR),
            hoy.get(Calendar.MONTH) + 1,
            hoy.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** Medianoche UTC del día local actual, como la usa el DatePicker de Material 3. */
    fun inicioDeHoyUtcMillis(): Long = formato("yyyy-MM-dd").parse(hoyIso())!!.time

    fun isoDesdeMillisUtc(millis: Long): String = formato("yyyy-MM-dd").format(Date(millis))

    fun millisUtcDesdeIso(iso: String): Long? =
        runCatching { formato("yyyy-MM-dd").parse(iso)?.time }.getOrNull()

    /** yyyy-MM-dd → dd/MM/yyyy para mostrar en pantalla. */
    fun paraMostrar(iso: String): String {
        val partes = iso.split("-")
        return if (partes.size == 3) "${partes[2]}/${partes[1]}/${partes[0]}" else iso
    }
}
