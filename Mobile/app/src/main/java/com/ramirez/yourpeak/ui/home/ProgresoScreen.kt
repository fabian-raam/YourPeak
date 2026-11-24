package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.RegistroProgresoResponse
import com.ramirez.yourpeak.data.repository.ProgresoRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FondoProgreso = Color.Black
private val FondoTarjetaProgreso = Color(0xFF101010)
private val BordeProgreso = Color(0xFF363636)
private val TextoSecundarioProgreso = Color(0xFFB8BCC6)
private val AzulProgreso = Color(0xFF2878FF)
private val VerdeProgreso = Color(0xFFB7F34A)
private val NaranjaProgreso = Color(0xFFFF6B35)

@Composable
fun ProgresoScreen(idUsuario: Long?, repository: ProgresoRepository) {
    val viewModel: ProgresoViewModel = viewModel(
        key = "progreso-$idUsuario",
        factory = ProgresoViewModel.factory(repository, idUsuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (estado.error != null || estado.aviso != null) {
            MensajeProgreso(
                texto = estado.error ?: estado.aviso.orEmpty(),
                esError = estado.error != null,
                cerrar = viewModel::cerrarMensajes
            )
        }

        EstadisticasProgreso(
            pesoActual = estado.pesoActual,
            variacion = estado.variacion,
            mediciones = estado.registros.size,
            cargando = estado.cargando
        )

        SeccionProgreso(titulo = "EVOLUCIÓN DEL PESO") {
            when {
                estado.cargando && estado.registros.isEmpty() -> Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = AzulProgreso, strokeWidth = 2.dp)
                    Text("  Cargando historial…", color = TextoSecundarioProgreso, fontSize = 15.sp)
                }
                estado.registros.isEmpty() -> Text(
                    "Registra tu primer peso para ver cómo evoluciona con el tiempo.",
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = TextoSecundarioProgreso,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
                else -> GraficoEvolucionPeso(estado.registros)
            }
        }

        SeccionProgreso(titulo = "REGISTRAR PESO") {
            Text(
                "Anota una nueva medición para actualizar tu progreso.",
                color = TextoSecundarioProgreso,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = estado.pesoIngresado,
                onValueChange = viewModel::cambiarPeso,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Peso (kg)", fontSize = 15.sp) },
                placeholder = { Text("Ejemplo: 72.5", fontSize = 15.sp) },
                singleLine = true,
                enabled = !estado.guardando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = viewModel::registrar,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !estado.guardando && !estado.cargando && estado.registros.isNotEmpty(),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = Color.White)
                    Text("  GUARDANDO…", fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 0.3.sp)
                } else {
                    Text("REGISTRAR PESO", fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 0.3.sp)
                }
            }
            if (estado.registros.isEmpty() && !estado.cargando) {
                Text(
                    "Completa primero tu perfil para registrar la medición inicial.",
                    modifier = Modifier.padding(top = 9.dp),
                    color = TextoSecundarioProgreso,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            }
        }

        SeccionProgreso(titulo = "HISTORIAL", trailing = "${estado.registros.size} MEDICIONES") {
            if (estado.registros.isEmpty()) {
                Text("Tus mediciones aparecerán aquí.", color = TextoSecundarioProgreso, fontSize = 15.sp)
            } else {
                estado.registros.take(10).forEach { FilaRegistroPeso(it) }
                if (estado.registros.size > 10) {
                    Text("Se muestran las 10 mediciones más recientes.", color = TextoSecundarioProgreso, fontSize = 14.sp)
                }
            }
        }

        if (!estado.cargando) {
            TextButton(onClick = viewModel::actualizar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("ACTUALIZAR HISTORIAL", color = AzulProgreso, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Tres bloques compactos con acentos de color, siguiendo la cuadrícula de Nutrición. */
@Composable
private fun EstadisticasProgreso(
    pesoActual: BigDecimal?,
    variacion: BigDecimal?,
    mediciones: Int,
    cargando: Boolean
) {
    val guion = if (cargando) "…" else "—"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = FondoProgreso,
        border = BorderStroke(1.dp, BordeProgreso)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            CeldaEstadisticaProgreso(
                titulo = "Peso actual",
                valor = pesoActual?.let(::formatearPeso) ?: guion,
                unidad = if (pesoActual != null) "kg" else "",
                acento = AzulProgreso,
                modifier = Modifier.weight(1f)
            )
            SeparadorProgreso()
            CeldaEstadisticaProgreso(
                titulo = "Variación",
                valor = variacion?.let(::variacionCorta) ?: guion,
                unidad = if (variacion != null) "kg" else "",
                acento = VerdeProgreso,
                modifier = Modifier.weight(1f)
            )
            SeparadorProgreso()
            CeldaEstadisticaProgreso(
                titulo = "Mediciones",
                valor = if (cargando) guion else mediciones.toString(),
                unidad = "",
                acento = NaranjaProgreso,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CeldaEstadisticaProgreso(
    titulo: String,
    valor: String,
    unidad: String,
    acento: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Spacer(Modifier.fillMaxWidth().height(2.dp).background(acento))
        Column(Modifier.padding(horizontal = 11.dp, vertical = 15.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(valor, color = Color.White, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black, maxLines = 1)
                if (unidad.isNotEmpty()) {
                    Spacer(Modifier.width(2.dp))
                    Text(unidad, color = TextoSecundarioProgreso, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(titulo, color = TextoSecundarioProgreso, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SeparadorProgreso() {
    Spacer(Modifier.fillMaxHeight().width(1.dp).background(BordeProgreso))
}

@Composable
private fun SeccionProgreso(titulo: String, trailing: String? = null, contenido: @Composable () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(titulo, modifier = Modifier.weight(1f), color = Color.White, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 0.35.sp)
            trailing?.let { Text(it, color = TextoSecundarioProgreso, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
        }
        HorizontalDivider(color = BordeProgreso)
        Spacer(Modifier.height(10.dp))
        contenido()
    }
}

/** El trazado se escala a partir de los pesos reales guardados, del más antiguo al más reciente. */
@Composable
private fun GraficoEvolucionPeso(registrosDescendentes: List<RegistroProgresoResponse>) {
    val puntos = registrosDescendentes.asReversed().mapNotNull { registro ->
        registro.pesoKg?.toFloat()?.let { registro to it }
    }
    if (puntos.isEmpty()) return

    val pesos = puntos.map { it.second }
    val minimo = pesos.minOrNull() ?: 0f
    val maximo = pesos.maxOrNull() ?: minimo
    val rango = (maximo - minimo).coerceAtLeast(1f)
    val base = (minimo - rango * 0.18f).coerceAtLeast(0f)
    val techo = maximo + rango * 0.18f
    val divisor = (techo - base).coerceAtLeast(1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = FondoTarjetaProgreso,
        border = BorderStroke(1.dp, BordeProgreso)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatearPeso(puntos.first().second) + " kg", color = TextoSecundarioProgreso, fontSize = 12.sp)
                Text(formatearPeso(puntos.last().second) + " kg", color = VerdeProgreso, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Canvas(Modifier.fillMaxWidth().height(94.dp).padding(horizontal = 5.dp, vertical = 7.dp)) {
                val left = 8.dp.toPx()
                val right = size.width - 8.dp.toPx()
                val top = 7.dp.toPx()
                val bottom = size.height - 7.dp.toPx()
                repeat(3) { index ->
                    val y = top + (bottom - top) * index / 2f
                    drawLine(BordeProgreso, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
                }
                val coords = puntos.mapIndexed { index, pair ->
                    val fractionX = if (puntos.size == 1) 0.5f else index.toFloat() / (puntos.size - 1)
                    val x = left + (right - left) * fractionX
                    val fractionY = ((pair.second - base) / divisor).coerceIn(0f, 1f)
                    val y = bottom - (bottom - top) * fractionY
                    Offset(x, y)
                }
                if (coords.size > 1) {
                    val path = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        coords.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, AzulProgreso, style = Stroke(width = 3.dp.toPx()))
                }
                coords.forEachIndexed { index, punto ->
                    val color = if (index == coords.lastIndex) VerdeProgreso else AzulProgreso
                    drawCircle(color, radius = if (index == coords.lastIndex) 4.dp.toPx() else 3.dp.toPx(), center = punto)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatearFechaCorta(puntos.first().first.fechaRegistro), color = TextoSecundarioProgreso, fontSize = 12.sp)
                if (puntos.size > 1) {
                    Text(formatearFechaCorta(puntos.last().first.fechaRegistro), color = TextoSecundarioProgreso, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FilaRegistroPeso(registro: RegistroProgresoResponse) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(formatearFecha(registro.fechaRegistro), color = TextoSecundarioProgreso, fontSize = 14.sp)
        Text(
            registro.pesoKg?.let { "${formatearPeso(it)} kg" } ?: "—",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider(color = BordeProgreso)
}

@Composable
private fun MensajeProgreso(texto: String, esError: Boolean, cerrar: () -> Unit) {
    val acento = if (esError) Color(0xFFFF6B6B) else VerdeProgreso
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF11130A),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, acento.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(start = 13.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("●", color = acento, fontSize = 16.sp)
            Text(texto, modifier = Modifier.weight(1f).padding(horizontal = 9.dp, vertical = 12.dp), color = Color.White, fontSize = 14.sp, lineHeight = 18.sp)
            TextButton(onClick = cerrar) { Text("Cerrar", color = AzulProgreso, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

private fun formatearPeso(peso: BigDecimal): String = peso.stripTrailingZeros().toPlainString()
private fun formatearPeso(peso: Float): String = BigDecimal.valueOf(peso.toDouble()).stripTrailingZeros().toPlainString()

private fun variacionCorta(variacion: BigDecimal): String {
    if (variacion.compareTo(BigDecimal.ZERO) == 0) return "0"
    val prefijo = if (variacion > BigDecimal.ZERO) "+" else ""
    return prefijo + formatearPeso(variacion)
}

private fun formatearFecha(fecha: String?): String {
    if (fecha.isNullOrBlank()) return "Fecha no disponible"
    val fechaLocal = runCatching { LocalDateTime.parse(fecha).toLocalDate() }
        .recoverCatching { LocalDate.parse(fecha.take(10)) }
        .getOrNull() ?: return fecha.take(10)
    return fechaLocal.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es-PE")))
}

private fun formatearFechaCorta(fecha: String?): String {
    if (fecha.isNullOrBlank()) return "—"
    val fechaLocal = runCatching { LocalDateTime.parse(fecha).toLocalDate() }
        .recoverCatching { LocalDate.parse(fecha.take(10)) }
        .getOrNull() ?: return fecha.take(10)
    return fechaLocal.format(DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es-PE")))
}
