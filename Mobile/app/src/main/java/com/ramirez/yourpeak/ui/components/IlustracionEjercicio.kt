package com.ramirez.yourpeak.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.ramirez.yourpeak.ui.theme.FondoIlustracion
import java.util.Locale

/**
 * Resuelve la clave imagenRuta que entrega la API a un recurso incluido en la app.
 * Convención: "aperturas-con-mancuernas" -> "exercise_aperturas_con_mancuernas".
 */
@Composable
fun recursoIlustracionEjercicio(imagenRuta: String?): Int? {
    val context = LocalContext.current
    return remember(context, imagenRuta) {
        val clave = imagenRuta
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace('-', '_')
            ?.takeIf { it.matches(Regex("[a-z0-9_]+")) }
            ?: return@remember null

        context.resources
            .getIdentifier("exercise_$clave", "drawable", context.packageName)
            .takeIf { it != 0 }
    }
}

/** Muestra la ilustración indicada por la ruta que viene asociada al ejercicio en la BD. */
@Composable
fun IlustracionEjercicio(
    imagenRuta: String?,
    grupoMuscular: String?,
    forma: Shape,
    modifier: Modifier = Modifier
) {
    val recurso = recursoIlustracionEjercicio(imagenRuta)
    Box(
        modifier = modifier.clip(forma).background(FondoIlustracion),
        contentAlignment = Alignment.Center
    ) {
        if (recurso != null) {
            Image(
                painter = painterResource(recurso),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize(0.3f)
                    .clip(CircleShape)
                    .background(colorGrupoMuscular(grupoMuscular.orEmpty()))
            )
        }
    }
}
