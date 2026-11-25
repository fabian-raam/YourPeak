package com.ramirez.yourpeak.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramirez.yourpeak.ui.theme.BarlowCondensed
import com.ramirez.yourpeak.ui.theme.Carbon
import com.ramirez.yourpeak.ui.theme.CarbonBorde

/** Piezas comunes del estilo «Azul sobre negro». */
val TextoSecundario = Color(0xFF9AA1AD)
val CarbonCampo = Color(0xFF1A1C21)
val NaranjaSube = Color(0xFFFF8A3D)

@Composable
fun TarjetaCarbon(
    modifier: Modifier = Modifier,
    borde: Color = CarbonBorde,
    relleno: Dp = 16.dp,
    contenido: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Carbon,
        border = BorderStroke(1.dp, borde)
    ) {
        Column(Modifier.fillMaxWidth().padding(relleno)) { contenido() }
    }
}

@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier,
        color = Color.White,
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 19.sp,
        letterSpacing = 0.8.sp
    )
}

/** Cifra grande condensada con su unidad pequeña, como en las tarjetas de resumen. */
@Composable
fun CifraGrande(valor: String, unidad: String? = null, color: Color = Color.White, tamano: Int = 30) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.Bottom) {
        Text(valor, color = color, fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = tamano.sp, lineHeight = (tamano + 2).sp)
        unidad?.let { Text(" $it", color = TextoSecundario, fontSize = 12.sp, modifier = Modifier.padding(bottom = 5.dp)) }
    }
}

/** Anillo de progreso con un valor y una leyenda en el centro. */
@Composable
fun AnilloProgreso(
    progreso: Float,
    centro: String,
    leyenda: String,
    color: Color,
    pista: Color = CarbonBorde,
    tamano: Dp = 104.dp,
    colorTexto: Color = Color.White,
    colorLeyenda: Color = TextoSecundario
) {
    Box(Modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(tamano - 8.dp)) {
            val grosor = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
            drawArc(pista, -90f, 360f, false, style = grosor)
            drawArc(color, -90f, 360f * progreso.coerceIn(0f, 1f), false, style = grosor)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centro, color = colorTexto, fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 24.sp)
            Text(leyenda, color = colorLeyenda, fontSize = 10.sp)
        }
    }
}

/** Etiqueta pequeña de estado (vigente, finalizada, −0.3, etc.). */
@Composable
fun EtiquetaEstado(texto: String, color: Color, fondo: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = fondo) {
        Text(texto, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
