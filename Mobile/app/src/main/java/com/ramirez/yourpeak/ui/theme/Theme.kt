package com.ramirez.yourpeak.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RosaPastel,
    onPrimary = Granate,
    primaryContainer = RosaPolvo,
    onPrimaryContainer = Granate,
    tertiary = VinoSuave,
    background = Crema,
    onBackground = Tinta,
    surface = Blanco,
    onSurface = Tinta,
    surfaceVariant = RosaNiebla,
    onSurfaceVariant = TintaSuave,
    outline = RosaBorde,
    error = ErrorSuave,
    errorContainer = ErrorFondo,
    onErrorContainer = ErrorSuave
)

private val DarkColorScheme = darkColorScheme(
    primary = RosaNoche,
    onPrimary = GranateNoche,
    primaryContainer = Color(0xFF4A2A2F),
    onPrimaryContainer = RosaNocheAcento,
    tertiary = RosaNocheAcento,
    background = Carbon,
    onBackground = TintaClara,
    surface = CarbonSuperficie,
    onSurface = TintaClara,
    surfaceVariant = CarbonCampo,
    onSurfaceVariant = TintaClaraSuave,
    outline = CarbonBorde,
    error = ErrorNoche,
    errorContainer = ErrorNocheFondo,
    onErrorContainer = ErrorNoche
)

/** Colores de estado que Material 3 no define (éxito y pendiente). */
@Immutable
data class EstadoColors(
    val exito: Color,
    val exitoFondo: Color,
    val pendiente: Color,
    val pendienteFondo: Color
)

private val EstadoClaro = EstadoColors(SalviaTexto, SalviaFondo, MielTexto, MielFondo)
private val EstadoOscuro = EstadoColors(SalviaNoche, SalviaNocheFondo, MielNoche, MielNocheFondo)

val LocalEstadoColors = staticCompositionLocalOf { EstadoClaro }

@Composable
fun YourPeakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Sin color dinámico: la app conserva siempre la paleta de la marca.
    CompositionLocalProvider(
        LocalEstadoColors provides if (darkTheme) EstadoOscuro else EstadoClaro
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
