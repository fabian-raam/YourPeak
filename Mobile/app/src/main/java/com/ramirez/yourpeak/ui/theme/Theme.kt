package com.ramirez.yourpeak.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Igual que la web (color-scheme: dark), la app usa siempre el tema oscuro de la marca.
private val YourPeakColorScheme = darkColorScheme(
    primary = Azul,
    onPrimary = Color.White,
    primaryContainer = AzulFondo,
    onPrimaryContainer = AzulClaro,
    tertiary = AzulEnlace,
    background = Marino,
    onBackground = Texto,
    surface = TarjetaFin,
    onSurface = Texto,
    surfaceVariant = Campo,
    onSurfaceVariant = TextoSuave,
    outline = CampoBorde,
    outlineVariant = TarjetaBorde,
    error = ErrorTexto,
    errorContainer = ErrorFondo,
    onErrorContainer = ErrorTexto
)

/** Colores que Material 3 no define: avisos de éxito/pendiente y el degradado de la tarjeta. */
@Immutable
data class EstadoColors(
    val exito: Color,
    val exitoFondo: Color,
    val pendiente: Color,
    val pendienteFondo: Color,
    val tarjetaInicio: Color,
    val tarjetaFin: Color,
    val placeholder: Color
)

private val Estados = EstadoColors(
    exito = ExitoTexto,
    exitoFondo = ExitoFondo,
    pendiente = PendienteTexto,
    pendienteFondo = PendienteFondo,
    tarjetaInicio = TarjetaInicio,
    tarjetaFin = TarjetaFin,
    placeholder = TextoPlaceholder
)

val LocalEstadoColors = staticCompositionLocalOf { Estados }

@Composable
fun YourPeakTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEstadoColors provides Estados) {
        MaterialTheme(
            colorScheme = YourPeakColorScheme,
            typography = Typography,
            content = content
        )
    }
}
