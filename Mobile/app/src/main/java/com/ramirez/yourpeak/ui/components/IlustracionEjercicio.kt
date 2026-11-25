package com.ramirez.yourpeak.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.ramirez.yourpeak.R
import com.ramirez.yourpeak.ui.theme.FondoIlustracion
import java.text.Normalizer

/** Mismas ilustraciones que el catálogo web (Frontend-web/react/src/assets/exercises). */
private val ILUSTRACIONES = mapOf(
    "aperturas_con_mancuernas" to R.drawable.ejercicio_aperturas_con_mancuernas,
    "curl_de_biceps" to R.drawable.ejercicio_curl_de_biceps,
    "elevaciones_laterales" to R.drawable.ejercicio_elevaciones_laterales,
    "extension_de_triceps_en_polea" to R.drawable.ejercicio_extension_de_triceps_en_polea,
    "flexiones_de_pecho" to R.drawable.ejercicio_flexiones_de_pecho,
    "jalon_al_pecho" to R.drawable.ejercicio_jalon_al_pecho,
    "peso_muerto_rumano" to R.drawable.ejercicio_peso_muerto_rumano,
    "plancha_frontal" to R.drawable.ejercicio_plancha_frontal,
    "prensa_de_piernas" to R.drawable.ejercicio_prensa_de_piernas,
    "press_de_banca" to R.drawable.ejercicio_press_de_banca,
    "press_inclinado_con_mancuernas" to R.drawable.ejercicio_press_inclinado_con_mancuernas,
    "press_militar" to R.drawable.ejercicio_press_militar,
    "remo_con_mancuerna" to R.drawable.ejercicio_remo_con_mancuerna,
    "saltos_de_tijera" to R.drawable.ejercicio_saltos_de_tijera,
    "sentadilla_goblet" to R.drawable.ejercicio_sentadilla_goblet,
    "zancadas_caminando" to R.drawable.ejercicio_zancadas_caminando
)

/** Convierte «Jalón al pecho» en «jalon_al_pecho» para buscar su ilustración. */
fun claveIlustracion(nombre: String?): String =
    Normalizer.normalize(nombre.orEmpty().trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

@DrawableRes
fun ilustracionEjercicio(nombre: String?): Int? = ILUSTRACIONES[claveIlustracion(nombre)]

/**
 * Muestra la ilustración del ejercicio sobre un fondo claro (las imágenes tienen fondo blanco).
 * Si el ejercicio no tiene ilustración, muestra un punto con el color de su grupo muscular.
 */
@Composable
fun IlustracionEjercicio(
    nombre: String?,
    grupoMuscular: String?,
    forma: Shape,
    modifier: Modifier = Modifier
) {
    val recurso = ilustracionEjercicio(nombre)
    Box(
        modifier = modifier.clip(forma).background(FondoIlustracion),
        contentAlignment = Alignment.Center
    ) {
        if (recurso != null) {
            Image(
                painter = painterResource(recurso),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
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
