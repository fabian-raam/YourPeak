package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.RegistroProgresoResponse
import com.ramirez.yourpeak.data.repository.ProgresoRepository
import com.ramirez.yourpeak.ui.components.CarbonCampo
import com.ramirez.yourpeak.ui.components.CifraGrande
import com.ramirez.yourpeak.ui.components.EtiquetaEstado
import com.ramirez.yourpeak.ui.components.NaranjaSube
import com.ramirez.yourpeak.ui.components.TarjetaCarbon
import com.ramirez.yourpeak.ui.components.TextoSecundario
import com.ramirez.yourpeak.ui.components.TituloSeccion
import com.ramirez.yourpeak.ui.theme.CarbonBorde
import com.ramirez.yourpeak.ui.theme.Lima
import com.ramirez.yourpeak.ui.theme.LimaFondo
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FondoSube = Color(0xFF2E1F12)
private val es = Locale.forLanguageTag("es-PE")

/** Rango de la gráfica: último mes, últimos tres meses o todo el historial. */
private enum class RangoPeso(val etiqueta: String, val meses: Long?) { MES("1M", 1), TRIMESTRE("3M", 3), TODO("Todo", null) }

@Composable
fun ProgresoScreen(idUsuario: Long?, repository: ProgresoRepository) {
    val viewModel: ProgresoViewModel = viewModel(
        key = "progreso-$idUsuario",
        factory = ProgresoViewModel.factory(repository, idUsuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var rango by rememberSaveable { mutableStateOf(RangoPeso.TODO) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        estado.error?.let { MensajeProgreso(it, true, viewModel::cerrarMensajes) }
        estado.aviso?.let { MensajeProgreso(it, false, viewModel::cerrarMensajes) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TarjetaCarbon(Modifier.weight(1f), relleno = 12.dp) {
                Text("Peso actual", color = TextoSecundario, fontSize = 12.sp)
                CifraGrande(estado.pesoActual?.let(::formatearPeso) ?: "—", if (estado.pesoActual != null) "kg" else null, tamano = 28)
            }
            TarjetaCarbon(Modifier.weight(1f), relleno = 12.dp) {
                Text("Variación", color = TextoSecundario, fontSize = 12.sp)
                val variacion = estado.variacion
                CifraGrande(
                    variacion?.let(::variacionCorta) ?: "—",
                    if (variacion != null) "kg" else null,
                    color = when {
                        variacion == null || variacion.signum() == 0 -> Color.White
                        variacion.signum() < 0 -> Lima
                        else -> NaranjaSube
                    },
                    tamano = 28
                )
            }
            TarjetaCarbon(Modifier.weight(1f), relleno = 12.dp) {
                Text("Mediciones", color = TextoSecundario, fontSize = 12.sp)
                CifraGrande(estado.registros.size.toString(), tamano = 28)
            }
        }

        TarjetaCarbon {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Evolución del peso", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                RangoPeso.entries.forEach { opcion ->
                    val elegido = opcion == rango
                    Surface(
                        modifier = Modifier.padding(start = 4.dp).clickable { rango = opcion },
                        shape = RoundedCornerShape(8.dp),
                        color = if (elegido) MaterialTheme.colorScheme.primary else Color.Transparent,
                        border = BorderStroke(1.dp, if (elegido) MaterialTheme.colorScheme.primary else CarbonBorde)
                    ) {
                        Text(opcion.etiqueta, Modifier.padding(horizontal = 9.dp, vertical = 4.dp), fontSize = 11.sp, color = if (elegido) Color.White else TextoSecundario)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val visibles = filtrarRango(estado.registros, rango)
            when {
                estado.cargando && estado.registros.isEmpty() -> Cargando("Cargando historial…")
                estado.registros.isEmpty() -> Text(
                    "Aún no hay mediciones. Completa tu perfil para guardar tu peso inicial.",
                    color = TextoSecundario, fontSize = 14.sp, lineHeight = 19.sp
                )
                visibles.size < 2 -> Text(
                    if (estado.registros.size < 2) "Registra otra medición para ver tu tendencia."
                    else "No hay suficientes mediciones en este periodo. Prueba con «Todo».",
                    color = TextoSecundario, fontSize = 14.sp, lineHeight = 19.sp,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
                else -> GraficoPeso(visibles)
            }
        }

        TarjetaCarbon(relleno = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = estado.pesoIngresado,
                    onValueChange = viewModel::cambiarPeso,
                    modifier = Modifier.weight(1f),
                    label = { Text("Nuevo peso (kg)") },
                    placeholder = { Text("Ej. 72.5") },
                    singleLine = true,
                    enabled = !estado.guardando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CarbonCampo,
                        unfocusedContainerColor = CarbonCampo,
                        disabledContainerColor = CarbonCampo,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = CarbonBorde
                    )
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = viewModel::registrar,
                    enabled = !estado.guardando && !estado.cargando && estado.registros.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    if (estado.guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("REGISTRAR", fontWeight = FontWeight.ExtraBold)
                }
            }
            if (estado.registros.isEmpty() && !estado.cargando) {
                Text(
                    "Primero completa tu perfil para registrar la medición inicial.",
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    color = TextoSecundario, fontSize = 12.sp
                )
            }
        }

        if (estado.registros.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                TituloSeccion("HISTORIAL", Modifier.weight(1f))
                TextButton(onClick = viewModel::actualizar) {
                    Text("Actualizar", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            TarjetaCarbon(relleno = 0.dp) {
                estado.registros.forEachIndexed { indice, registro ->
                    if (indice > 0) HorizontalDivider(color = CarbonBorde, modifier = Modifier.padding(horizontal = 14.dp))
                    FilaRegistroPeso(registro, estado.registros.getOrNull(indice + 1))
                }
            }
        }
    }
}

/** Línea azul con relleno degradado; el último punto (el más reciente) va en lima. */
@Composable
private fun GraficoPeso(registrosDescendentes: List<RegistroProgresoResponse>) {
    val puntos = registrosDescendentes.reversed().mapNotNull { registro -> registro.pesoKg?.let { registro to it.toFloat() } }
    val minimo = puntos.minOf { it.second }
    val maximo = puntos.maxOf { it.second }
    val primario = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${formatearPeso(maximo)} kg", color = TextoSecundario, fontSize = 11.sp)
        Text("${formatearPeso(puntos.last().second)} kg", color = Lima, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    Canvas(Modifier.fillMaxWidth().height(130.dp).padding(vertical = 8.dp)) {
        val margen = if (maximo - minimo < 0.5f) 0.5f else (maximo - minimo) * 0.15f
        val base = minimo - margen
        val rangoY = (maximo + margen) - base
        val coords = puntos.mapIndexed { indice, (_, peso) ->
            Offset(
                x = 6.dp.toPx() + (size.width - 12.dp.toPx()) * indice / (puntos.size - 1),
                y = size.height * (1f - (peso - base) / rangoY)
            )
        }
        val linea = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            coords.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(linea)
            lineTo(coords.last().x, size.height)
            lineTo(coords.first().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(primario.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(linea, primario, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))
        coords.forEachIndexed { indice, punto ->
            val ultimo = indice == coords.lastIndex
            drawCircle(if (ultimo) Lima else primario, radius = if (ultimo) 5.dp.toPx() else 3.dp.toPx(), center = punto)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatearFecha(puntos.first().first.fechaRegistro, "d MMM"), color = TextoSecundario, fontSize = 11.sp)
        Text(formatearFecha(puntos.last().first.fechaRegistro, "d MMM"), color = TextoSecundario, fontSize = 11.sp)
    }
}

/** Fila del historial con la diferencia frente a la medición anterior. */
@Composable
private fun FilaRegistroPeso(registro: RegistroProgresoResponse, anterior: RegistroProgresoResponse?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(formatearFecha(registro.fechaRegistro, "d MMM yyyy"), color = TextoSecundario, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(registro.pesoKg?.let { "${formatearPeso(it)} kg" } ?: "—", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        val diferencia = registro.pesoKg?.let { actual -> anterior?.pesoKg?.let { actual.subtract(it).stripTrailingZeros() } }
        if (diferencia != null && diferencia.signum() != 0) {
            Spacer(Modifier.width(8.dp))
            if (diferencia.signum() < 0) EtiquetaEstado(variacionCorta(diferencia), Lima, LimaFondo)
            else EtiquetaEstado(variacionCorta(diferencia), NaranjaSube, FondoSube)
        }
    }
}

@Composable
private fun Cargando(texto: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text("  $texto", color = TextoSecundario, fontSize = 14.sp)
    }
}

@Composable
private fun MensajeProgreso(texto: String, esError: Boolean, cerrar: () -> Unit) {
    val acento = if (esError) Color(0xFFFF6B6B) else Lima
    TarjetaCarbon(borde = acento.copy(alpha = 0.55f), relleno = 4.dp) {
        Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = acento, fontSize = 14.sp)
            Text(texto, modifier = Modifier.weight(1f).padding(horizontal = 9.dp, vertical = 8.dp), color = Color.White, fontSize = 14.sp, lineHeight = 18.sp)
            TextButton(onClick = cerrar) { Text("Cerrar", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

private fun filtrarRango(registros: List<RegistroProgresoResponse>, rango: RangoPeso): List<RegistroProgresoResponse> {
    val meses = rango.meses ?: return registros
    val limite = LocalDate.now().minusMonths(meses)
    return registros.filter { fechaLocal(it.fechaRegistro)?.isBefore(limite) == false }
}

private fun formatearPeso(peso: BigDecimal): String = peso.stripTrailingZeros().toPlainString()
private fun formatearPeso(peso: Float): String = BigDecimal.valueOf(peso.toDouble()).setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

private fun variacionCorta(variacion: BigDecimal): String {
    if (variacion.signum() == 0) return "0"
    return (if (variacion.signum() > 0) "+" else "−") + formatearPeso(variacion.abs())
}

private fun fechaLocal(fecha: String?): LocalDate? {
    if (fecha.isNullOrBlank()) return null
    return runCatching { LocalDateTime.parse(fecha).toLocalDate() }
        .recoverCatching { LocalDate.parse(fecha.take(10)) }
        .getOrNull()
}

private fun formatearFecha(fecha: String?, patron: String): String =
    fechaLocal(fecha)?.format(DateTimeFormatter.ofPattern(patron, es)) ?: (fecha?.take(10) ?: "—")
