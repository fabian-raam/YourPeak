package com.ramirez.yourpeak.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private fun estiloTrazo(grosor: Float) = Stroke(width = grosor, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
fun IconoInicio(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(25.dp)) {
        val escala = size.width / 24f
        val casa = Path().apply {
            moveTo(4f * escala, 11f * escala)
            lineTo(12f * escala, 4f * escala)
            lineTo(20f * escala, 11f * escala)
            moveTo(6.5f * escala, 10f * escala)
            lineTo(6.5f * escala, 20f * escala)
            lineTo(17.5f * escala, 20f * escala)
            lineTo(17.5f * escala, 10f * escala)
            moveTo(10f * escala, 20f * escala)
            lineTo(10f * escala, 14f * escala)
            lineTo(14f * escala, 14f * escala)
            lineTo(14f * escala, 20f * escala)
        }
        drawPath(casa, color, style = estiloTrazo(1.8.dp.toPx()))
    }
}

@Composable
fun IconoEntrenamiento(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(25.dp)) {
        val escala = size.width / 24f
        drawLine(color, Offset(5f * escala, 12f * escala), Offset(19f * escala, 12f * escala), 1.8.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(4f * escala, 8f * escala), Offset(4f * escala, 16f * escala), 2.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(7f * escala, 9f * escala), Offset(7f * escala, 15f * escala), 2.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(17f * escala, 9f * escala), Offset(17f * escala, 15f * escala), 2.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(20f * escala, 8f * escala), Offset(20f * escala, 16f * escala), 2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
fun IconoNutricion(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(25.dp)) {
        val escala = size.width / 24f
        val manzana = Path().apply {
            moveTo(12f * escala, 8f * escala)
            cubicTo(8f * escala, 5f * escala, 4f * escala, 8f * escala, 5f * escala, 13f * escala)
            cubicTo(6f * escala, 18f * escala, 8f * escala, 21f * escala, 11f * escala, 20f * escala)
            cubicTo(12f * escala, 19.5f * escala, 12f * escala, 19.5f * escala, 13f * escala, 20f * escala)
            cubicTo(16f * escala, 21f * escala, 18f * escala, 18f * escala, 19f * escala, 13f * escala)
            cubicTo(20f * escala, 8f * escala, 16f * escala, 5f * escala, 12f * escala, 8f * escala)
        }
        val hoja = Path().apply {
            moveTo(12f * escala, 7f * escala)
            cubicTo(12f * escala, 4f * escala, 15f * escala, 3f * escala, 17f * escala, 4f * escala)
            cubicTo(17f * escala, 6f * escala, 15f * escala, 8f * escala, 12f * escala, 7f * escala)
        }
        drawPath(manzana, color, style = estiloTrazo(1.8.dp.toPx()))
        drawPath(hoja, color, style = estiloTrazo(1.8.dp.toPx()))
        drawLine(color, Offset(12f * escala, 7f * escala), Offset(12f * escala, 9f * escala), 1.6.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
fun IconoProgreso(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(25.dp)) {
        val escala = size.width / 24f
        val grafico = Path().apply {
            moveTo(4f * escala, 18f * escala)
            lineTo(9f * escala, 13f * escala)
            lineTo(12f * escala, 15f * escala)
            lineTo(19.5f * escala, 7f * escala)
        }
        drawPath(grafico, color, style = estiloTrazo(1.8.dp.toPx()))
        drawLine(color, Offset(15.5f * escala, 7f * escala), Offset(19.5f * escala, 7f * escala), 1.8.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(19.5f * escala, 7f * escala), Offset(19.5f * escala, 11f * escala), 1.8.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(4f * escala, 20f * escala), Offset(20f * escala, 20f * escala), 1.4.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
fun IconoMembresia(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(25.dp)) {
        val escala = size.width / 24f
        drawRoundRect(
            color = color,
            topLeft = Offset(3.5f * escala, 6f * escala),
            size = Size(17f * escala, 12f * escala),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * escala),
            style = estiloTrazo(1.8.dp.toPx())
        )
        drawLine(color, Offset(4.5f * escala, 10f * escala), Offset(19.5f * escala, 10f * escala), 1.6.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(7f * escala, 14f * escala), Offset(10f * escala, 14f * escala), 1.6.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
fun IconoPerfil(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(25.dp)) {
        val escala = size.width / 24f
        drawCircle(
            color = color,
            radius = 3.2f * escala,
            center = Offset(12f * escala, 8f * escala),
            style = estiloTrazo(1.8.dp.toPx())
        )
        val busto = Path().apply {
            moveTo(5f * escala, 20f * escala)
            cubicTo(5.5f * escala, 15.5f * escala, 8f * escala, 13f * escala, 12f * escala, 13f * escala)
            cubicTo(16f * escala, 13f * escala, 18.5f * escala, 15.5f * escala, 19f * escala, 20f * escala)
        }
        drawPath(busto, color, style = estiloTrazo(1.8.dp.toPx()))
    }
}
