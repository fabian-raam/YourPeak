package com.ramirez.yourpeak.ui.components

import androidx.compose.ui.graphics.Color

/** Mantiene un mismo color identificador para cada grupo muscular en la app. */
fun colorGrupoMuscular(grupoMuscular: String?): Color = when (grupoMuscular.orEmpty().lowercase()) {
    "pecho" -> Color(0xFF2677FF)
    "brazos" -> Color(0xFFFF5A36)
    "piernas" -> Color(0xFFB7F34A)
    "espalda" -> Color.White
    "core" -> Color(0xFFB7F34A)
    else -> Color(0xFFB7F34A)
}
