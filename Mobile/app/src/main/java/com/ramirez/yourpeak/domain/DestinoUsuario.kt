package com.ramirez.yourpeak.domain

import java.util.Locale

/** Destinos permitidos después de leer el rol y estado devueltos por Spring. */
enum class DestinoUsuario {
    INICIO_CLIENTE,
    CUENTA_PENDIENTE,
    CUENTA_INACTIVA,
    INFORMACION_ADMINISTRADOR,
    ACCESO_NO_RECONOCIDO
}

/** Resuelve el flujo inicial sin depender de Compose ni de la navegación. */
fun determinarDestinoUsuario(rol: String?, estado: String?): DestinoUsuario {
    val rolNormalizado = rol?.trim()?.uppercase(Locale.ROOT)
    val estadoNormalizado = estado?.trim()?.uppercase(Locale.ROOT)

    return when (rolNormalizado) {
        "ADMINISTRADOR" -> DestinoUsuario.INFORMACION_ADMINISTRADOR
        "CLIENTE" -> when (estadoNormalizado) {
            "ACTIVO" -> DestinoUsuario.INICIO_CLIENTE
            "PENDIENTE" -> DestinoUsuario.CUENTA_PENDIENTE
            "INACTIVO" -> DestinoUsuario.CUENTA_INACTIVA
            else -> DestinoUsuario.ACCESO_NO_RECONOCIDO
        }
        else -> DestinoUsuario.ACCESO_NO_RECONOCIDO
    }
}
