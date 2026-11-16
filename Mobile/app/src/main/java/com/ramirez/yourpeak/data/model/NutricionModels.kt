package com.ramirez.yourpeak.data.model

import java.math.BigDecimal

/** Alimento y valores nutricionales por porción base del catálogo de Spring. */
data class AlimentoNutricionResponse(
    val idAlimento: Int?,
    val nombre: String?,
    val porcionBaseG: BigDecimal?,
    val kcal: BigDecimal?,
    val proteinaG: BigDecimal?,
    val carbohidratoG: BigDecimal?,
    val grasaG: BigDecimal?
)

/** Consumo registrado, con sus valores ya escalados a la cantidad consumida. */
data class IngestaNutricionResponse(
    val idIngesta: Long?,
    val idAlimento: Int?,
    val alimento: String?,
    val cantidadG: BigDecimal?,
    val fechaHora: String?,
    val kcal: BigDecimal?,
    val proteinaG: BigDecimal?,
    val carbohidratoG: BigDecimal?,
    val grasaG: BigDecimal?
)

/** Totales y detalle diario entregados por el backend. */
data class ResumenNutricionalResponse(
    val fecha: String?,
    val kcal: BigDecimal?,
    val proteinaG: BigDecimal?,
    val carbohidratoG: BigDecimal?,
    val grasaG: BigDecimal?,
    val ingestas: List<IngestaNutricionResponse>?
)

/** Datos que el backend necesita para crear o actualizar un consumo. */
data class RegistrarIngestaRequest(
    val idAlimento: Int,
    val cantidadG: BigDecimal
)
