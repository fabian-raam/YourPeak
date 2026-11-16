package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.RegistroProgresoResponse
import com.ramirez.yourpeak.data.repository.ProgresoRepository
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgresoScreen(idUsuario: Long?, repository: ProgresoRepository) {
    val viewModel: ProgresoViewModel = viewModel(
        key = "progreso-$idUsuario",
        factory = ProgresoViewModel.factory(repository, idUsuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val variacion = estado.variacion

    Column {
        EncabezadoTarjeta(
            etiqueta = "SEGUIMIENTO PERSONAL",
            titulo = "Mi progreso",
            introduccion = "Registra tu peso y observa cómo cambia con el tiempo."
        )
        estado.error?.let { MensajeProgreso(it, true, viewModel::cerrarMensajes) }
        estado.aviso?.let { MensajeProgreso(it, false, viewModel::cerrarMensajes) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ResumenProgreso(
                titulo = "Peso actual",
                valor = estado.pesoActual?.let { "${formatearPeso(it)} kg" } ?: "—",
                modifier = Modifier.weight(1f)
            )
            ResumenProgreso(
                titulo = "Variación desde el inicio",
                valor = variacion?.let(::variacionTexto) ?: "—",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        ResumenProgreso(
            titulo = "Mediciones registradas",
            valor = estado.registros.size.toString(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Evolución del peso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Tus mediciones guardadas por fecha.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                when {
                    estado.cargando && estado.registros.isEmpty() -> Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("  Cargando historial…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    estado.registros.isEmpty() -> Text(
                        "Aún no hay mediciones. Completa primero tu perfil para guardar tu peso inicial.",
                        modifier = Modifier.padding(vertical = 18.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> GraficoPeso(estado.registros)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Registrar peso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cada registro se guarda con la fecha y hora actuales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = estado.pesoIngresado,
                    onValueChange = viewModel::cambiarPeso,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Peso (kg)") },
                    placeholder = { Text("Ejemplo: 72.5") },
                    singleLine = true,
                    enabled = !estado.guardando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = viewModel::registrar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !estado.guardando && !estado.cargando && estado.registros.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (estado.guardando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Text("  Guardando…")
                    } else Text("Registrar peso")
                }
                if (estado.registros.isEmpty() && !estado.cargando) {
                    Text(
                        "Primero completa tu perfil para registrar la medición inicial.",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (estado.registros.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Historial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    estado.registros.take(10).forEach { FilaRegistroPeso(it) }
                    if (estado.registros.size > 10) {
                        Text("Se muestran las 10 mediciones más recientes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (!estado.cargando) {
            TextButton(onClick = viewModel::actualizar, modifier = Modifier.align(Alignment.End)) {
                Text("Actualizar historial")
            }
        }
    }
}

@Composable
private fun ResumenProgreso(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun GraficoPeso(registrosDescendentes: List<RegistroProgresoResponse>) {
    val puntos = registrosDescendentes.asReversed().mapNotNull { registro ->
        registro.pesoKg?.toFloat()?.let { peso -> registro to peso }
    }
    if (puntos.isEmpty()) return
    val azul = MaterialTheme.colorScheme.tertiary
    val texto = MaterialTheme.colorScheme.onSurfaceVariant
    val pesos = puntos.map { it.second }
    val rangoMin = pesos.minOrNull() ?: 0f
    val rangoMax = pesos.maxOrNull() ?: rangoMin
    val margen = ((rangoMax - rangoMin) * 0.2f).coerceAtLeast(1f)
    val bajo = rangoMin - margen
    val alto = rangoMax + margen

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${formatearPeso(BigDecimal.valueOf(alto.toDouble()))} kg", style = MaterialTheme.typography.labelSmall, color = texto)
            Text("${formatearPeso(BigDecimal.valueOf(bajo.toDouble()))} kg", style = MaterialTheme.typography.labelSmall, color = texto)
        }
        Canvas(Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 8.dp, vertical = 8.dp)) {
            val xStep = if (puntos.size <= 1) 0f else size.width / (puntos.size - 1)
            val path = Path()
            val coords = puntos.mapIndexed { index, pair ->
                val x = index * xStep
                val y = size.height - ((pair.second - bajo) / (alto - bajo)) * size.height
                x to y
            }
            coords.forEachIndexed { index, (x, y) ->
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, azul, style = Stroke(width = 4.dp.toPx()))
            coords.forEach { (x, y) -> drawCircle(azul, radius = 5.dp.toPx(), center = Offset(x, y)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatearFecha(puntos.first().first.fechaRegistro), style = MaterialTheme.typography.labelSmall, color = texto)
            if (puntos.size > 1) Text(formatearFecha(puntos.last().first.fechaRegistro), style = MaterialTheme.typography.labelSmall, color = texto)
        }
    }
}

@Composable
private fun FilaRegistroPeso(registro: RegistroProgresoResponse) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(formatearFecha(registro.fechaRegistro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(registro.pesoKg?.let { "${formatearPeso(it)} kg" } ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun MensajeProgreso(texto: String, error: Boolean, cerrar: () -> Unit) {
    val color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(texto, modifier = Modifier.weight(1f).padding(vertical = 10.dp), color = color, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = cerrar) { Text("×", color = color) }
        }
    }
}

private fun formatearPeso(peso: BigDecimal): String = peso.stripTrailingZeros().toPlainString()

private fun variacionTexto(variacion: BigDecimal): String {
    if (variacion.compareTo(BigDecimal.ZERO) == 0) return "0 kg"
    val prefijo = if (variacion > BigDecimal.ZERO) "+" else ""
    return "$prefijo${formatearPeso(variacion)} kg"
}

private fun formatearFecha(fecha: String?): String {
    if (fecha.isNullOrBlank()) return "Fecha no disponible"
    val fechaLocal = runCatching { LocalDateTime.parse(fecha).toLocalDate() }
        .recoverCatching { LocalDate.parse(fecha.take(10)) }
        .getOrNull() ?: return fecha.take(10)
    return fechaLocal.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es-PE")))
}
