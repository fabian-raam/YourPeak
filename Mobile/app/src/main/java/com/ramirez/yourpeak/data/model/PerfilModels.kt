package com.ramirez.yourpeak.data.model

import java.math.BigDecimal

/** Perfil inicial del cliente tal como lo devuelve Spring en /api/cliente/perfil. */
data class PerfilInicialResponse(
    val completado: Boolean?,
    val idTipoObjetivo: Int?,
    val objetivo: String?,
    val idNivelExperiencia: Int?,
    val nivelExperiencia: String?,
    val idNivelActividad: Int?,
    val nivelActividad: String?,
    val sexo: String?,
    val pesoKg: BigDecimal?,
    val tallaCm: BigDecimal?,
    val fechaRegistroFisico: String?,
    val catalogos: PerfilCatalogosResponse?
)

data class PerfilCatalogosResponse(
    val objetivos: List<OpcionPerfilResponse>?,
    val nivelesExperiencia: List<OpcionPerfilResponse>?,
    val nivelesActividad: List<OpcionPerfilResponse>?
)

data class OpcionPerfilResponse(
    val id: Int?,
    val nombre: String?,
    val descripcion: String?
)

/** El sexo solo puede registrarse una vez; si cambian peso o talla, Spring agrega una medición. */
data class PerfilInicialRequest(
    val idTipoObjetivo: Int,
    val idNivelExperiencia: Int,
    val idNivelActividad: Int,
    val sexo: String,
    val pesoKg: BigDecimal,
    val tallaCm: BigDecimal
)
