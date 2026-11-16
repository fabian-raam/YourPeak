package com.ramirez.yourpeak.data.model

import java.math.BigDecimal

/** Representa una medición de peso del historial devuelto por Spring. */
data class RegistroProgresoResponse(
    val idRegistroFisico: Long?,
    val pesoKg: BigDecimal?,
    val tallaCm: BigDecimal?,
    val fechaRegistro: String?
)

/** Datos que se envían al registrar un nuevo peso. */
data class RegistroProgresoRequest(
    val pesoKg: BigDecimal
)
