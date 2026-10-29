package com.ramirez.yourpeak.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramirez.yourpeak.R
import com.ramirez.yourpeak.ui.components.BotonPrincipal
import com.ramirez.yourpeak.ui.theme.AzulAcento
import com.ramirez.yourpeak.ui.theme.YourPeakTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Misma composición que el hero del login web (AuthHero.jsx): degradado de fondo,
// frase "BUSCA TU MEJOR VERSIÓN.", forma azul inclinada y el atleta.
private val FondoCentro = Color(0xFF1C1E26)
private val FondoMedio = Color(0xFF131621)
private val FondoBorde = Color(0xFF10131C)
private val TextoTenue = Color(0xFFA0A6CA)

/** Triángulo irregular de la web: clip-path polygon(0 22%, 100% 0, 70% 100%). */
private val FormaHero = GenericShape { tamano, _ ->
    moveTo(0f, tamano.height * 0.22f)
    lineTo(tamano.width, 0f)
    lineTo(tamano.width * 0.70f, tamano.height)
    close()
}

/**
 * Pantalla de bienvenida. Con sesión guardada continúa sola al inicio;
 * sin sesión ofrece ir directamente al login o al registro.
 */
@Composable
fun SplashScreen(
    haySesion: Boolean,
    alContinuarConSesion: () -> Unit,
    alIniciarSesion: () -> Unit,
    alRegistrarse: () -> Unit
) {
    val opacidadAtleta = remember { Animatable(0f) }
    val opacidadTexto = remember { Animatable(0f) }
    val opacidadBotones = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { opacidadAtleta.animateTo(1f, tween(850)) }
        opacidadTexto.animateTo(1f, tween(600, delayMillis = 150))
        if (haySesion) {
            delay(1100)
            alContinuarConSesion()
        } else {
            opacidadBotones.animateTo(1f, tween(400))
        }
    }
    SplashContenido(
        opacidadAtleta = opacidadAtleta.value,
        opacidadTexto = opacidadTexto.value,
        opacidadBotones = opacidadBotones.value,
        mostrarBotones = !haySesion,
        alIniciarSesion = alIniciarSesion,
        alRegistrarse = alRegistrarse
    )
}

@Composable
private fun SplashContenido(
    opacidadAtleta: Float,
    opacidadTexto: Float,
    opacidadBotones: Float,
    mostrarBotones: Boolean,
    alIniciarSesion: () -> Unit,
    alRegistrarse: () -> Unit
) {
    val colores = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val densidad = LocalDensity.current
        val anchoPx = with(densidad) { maxWidth.toPx() }
        val altoPx = with(densidad) { maxHeight.toPx() }
        val alto = maxHeight
        // En pantallas bajas se reduce el título para que los botones siempre quepan.
        val pantallaBaja = maxHeight < 720.dp

        // En pantallas bajas el atleta y la forma se achican para no tapar la frase.
        val escalaAtleta = if (maxHeight < 720.dp) 0.62f else 0.82f

        // Degradado radial suave detrás del atleta, como en la web
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0f to FondoCentro,
                        0.42f to FondoMedio,
                        0.72f to FondoBorde,
                        center = Offset(anchoPx * 0.70f, altoPx * 0.55f),
                        radius = maxOf(anchoPx, altoPx) * 0.75f
                    )
                )
        )

        // Forma azul detrás del atleta
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = maxWidth * 0.02f, y = maxHeight * (0.42f + (0.82f - escalaAtleta) * 0.5f))
                .width(maxWidth * (escalaAtleta - 0.04f))
                .height(maxHeight * 0.36f * escalaAtleta / 0.82f)
                .alpha(opacidadAtleta)
                .rotate(-9f)
                .clip(FormaHero)
                .background(Brush.linearGradient(listOf(AzulAcento, colores.primary)))
        )

        Image(
            painter = painterResource(R.drawable.atleta),
            contentDescription = "Atleta de Your Peak",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(escalaAtleta)
                .aspectRatio(408f / 612f)
                .alpha(opacidadAtleta)
        )

        if (mostrarBotones) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(alto * 0.38f)
                    .alpha(opacidadBotones)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, FondoBorde.copy(alpha = 0.92f), FondoBorde)))
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 26.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    tint = colores.onBackground,
                    modifier = Modifier.size(width = 18.dp, height = 30.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text("YOUR PEAK", color = colores.onBackground, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }

            Spacer(Modifier.height(alto * if (pantallaBaja) 0.05f else 0.11f))

            Column(Modifier.alpha(opacidadTexto)) {
                Text(
                    "CADA DÍA, UN PASO MÁS",
                    color = TextoTenue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    buildAnnotatedString {
                        append("BUSCA TU\n")
                        withStyle(SpanStyle(color = AzulAcento)) { append("MEJOR") }
                        append("\nVERSIÓN.")
                    },
                    color = colores.onBackground,
                    fontSize = if (pantallaBaja) 40.sp else 52.sp,
                    lineHeight = if (pantallaBaja) 40.sp else 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Entrena con propósito.\nSupera tus límites.",
                    color = colores.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.weight(1f))

            if (mostrarBotones) {
                Column(Modifier.padding(top = 20.dp).alpha(opacidadBotones)) {
                    BotonPrincipal("Iniciar sesión", alIniciarSesion)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = alRegistrarse,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colores.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = FondoBorde.copy(alpha = 0.6f),
                            contentColor = colores.onBackground
                        )
                    ) {
                        Text("Crear cuenta", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun SplashPreview() {
    YourPeakTheme {
        SplashContenido(
            opacidadAtleta = 1f, opacidadTexto = 1f, opacidadBotones = 1f,
            mostrarBotones = true, alIniciarSesion = {}, alRegistrarse = {}
        )
    }
}
