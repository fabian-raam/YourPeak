package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.AlimentoNutricionResponse
import com.ramirez.yourpeak.data.repository.NutricionRepository
import com.ramirez.yourpeak.domain.Fechas
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.theme.YourPeakTheme
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun NutricionScreen(idUsuario: Long?, repository: NutricionRepository) {
    val viewModel: NutricionViewModel = viewModel(
        key = "nutricion-$idUsuario",
        factory = NutricionViewModel.factory(repository, idUsuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var mostrarSelectorAlimento by remember { mutableStateOf(false) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    val ingestaPorEliminar = estado.ingestas.firstOrNull { it.idIngesta == estado.idIngestaPorEliminar }
    val alimentoSeleccionado = estado.alimentoSeleccionado
    val cantidadPrevia = estado.cantidadG.replace(',', '.').toBigDecimalOrNull()
    val nutrientesPrevia = remember(alimentoSeleccionado, cantidadPrevia) {
        alimentoSeleccionado?.let { alimento -> calcularNutrientes(alimento, cantidadPrevia) }
    }

    Column {
        EncabezadoTarjeta(
            etiqueta = "CLIENTE",
            titulo = "Nutrición",
            introduccion = "Registra lo que comes y revisa el resumen de cada día."
        )
        Spacer(Modifier.height(14.dp))

        if (estado.error != null || estado.errorCatalogo != null || estado.aviso != null) {
            MensajeNutricion(
                texto = estado.error ?: estado.errorCatalogo ?: estado.aviso.orEmpty(),
                esError = estado.error != null || estado.errorCatalogo != null,
                cerrar = viewModel::cerrarMensajes
            )
            Spacer(Modifier.height(12.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResumenNutricion("Calorías", estado.kcal, "kcal", estado.cargandoDia, Modifier.weight(1f))
                ResumenNutricion("Proteína", estado.proteinaG, "g", estado.cargandoDia, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResumenNutricion("Carbohidratos", estado.carbohidratoG, "g", estado.cargandoDia, Modifier.weight(1f))
                ResumenNutricion("Grasas", estado.grasaG, "g", estado.cargandoDia, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                Column(Modifier.padding(16.dp)) {
                    Text("Registro diario", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Los consumos se agrupan por fecha, sin separar por comidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { viewModel.moverDia(-1) }) { Text("‹") }
                        Column(
                            modifier = Modifier.weight(1f).clickable { mostrarCalendario = true }.padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                formatearFecha(estado.fecha),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = { mostrarCalendario = true }) { Text("Elegir fecha") }
                        }
                        OutlinedButton(
                            onClick = { viewModel.moverDia(1) },
                            enabled = !estado.esHoy
                        ) { Text("›") }
                    }
                }

                if (estado.esHoy) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Agregar alimento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (estado.cargandoCatalogo) "Cargando catálogo..."
                                    else "Busca un alimento y registra la cantidad que consumiste.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = estado.busqueda,
                                    onValueChange = viewModel::buscar,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Buscar alimento") },
                                    singleLine = true,
                                    enabled = !estado.cargandoCatalogo && !estado.guardando
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { mostrarSelectorAlimento = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !estado.cargandoCatalogo && estado.alimentos.isNotEmpty() && !estado.guardando
                                ) {
                                    Text(alimentoSeleccionado?.nombre ?: "Selecciona un alimento")
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = estado.cantidadG,
                                    onValueChange = viewModel::cambiarCantidad,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Cantidad (g)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    enabled = !estado.guardando
                                )
                                nutrientesPrevia?.let {
                                    Text(
                                        "Aproximado: ${formatearNumero(it[0])} kcal · ${formatearNumero(it[1])} g proteína · ${formatearNumero(it[2])} g carbohidratos · ${formatearNumero(it[3])} g grasas",
                                        modifier = Modifier.padding(top = 8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (estado.alimentos.isEmpty() && !estado.cargandoCatalogo) {
                                    TextButton(onClick = viewModel::reintentarCatalogo) { Text("Volver a cargar alimentos") }
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = viewModel::registrar,
                                    modifier = Modifier.align(Alignment.End),
                                    enabled = alimentoSeleccionado != null && !estado.guardando && !estado.cargandoCatalogo
                                ) {
                                    if (estado.guardando) {
                                        CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text("Agregar")
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Día anterior: puedes revisar, corregir o eliminar consumos registrados.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alimentos registrados", fontWeight = FontWeight.SemiBold)
                    Text("${estado.ingestas.size} ${if (estado.ingestas.size == 1) "consumo" else "consumos"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                when {
                    estado.cargandoDia -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(18.dp))
                    estado.ingestas.isEmpty() -> Text(
                        if (estado.esHoy) "Aún no registraste alimentos hoy." else "No hay consumos en esta fecha.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> estado.ingestas.forEach { ingesta ->
                        IngestaFila(
                            ingesta = ingesta,
                            editando = estado.idIngestaEditando == ingesta.idIngesta,
                            cantidadEditada = estado.cantidadEditadaG,
                            guardando = estado.guardando,
                            cambiarCantidad = viewModel::cambiarCantidadEditada,
                            iniciarEdicion = { viewModel.iniciarEdicion(ingesta) },
                            guardarEdicion = { viewModel.guardarEdicion(ingesta) },
                            cancelarEdicion = viewModel::cancelarEdicion,
                            eliminar = { viewModel.pedirEliminar(ingesta) }
                        )
                    }
                }
            }
        }
    }

    if (mostrarSelectorAlimento) {
        SelectorAlimentoDialog(
            alimentos = estado.alimentos.filter { alimento ->
                val query = estado.busqueda.trim()
                query.isBlank() || alimento.nombre.orEmpty().contains(query, ignoreCase = true)
            },
            seleccionar = { id ->
                viewModel.seleccionarAlimento(id)
                mostrarSelectorAlimento = false
            },
            cerrar = { mostrarSelectorAlimento = false }
        )
    }

    if (mostrarCalendario) {
        CalendarioNutricion(
            fecha = estado.fecha,
            seleccionar = { fecha -> viewModel.actualizarDia(fecha); mostrarCalendario = false },
            cerrar = { mostrarCalendario = false }
        )
    }

    if (ingestaPorEliminar != null) {
        AlertDialog(
            onDismissRequest = { viewModel.pedirEliminar(null) },
            title = { Text("Eliminar consumo") },
            text = { Text("¿Quieres eliminar ${ingestaPorEliminar.alimento} del registro?") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarEliminar, enabled = !estado.guardando) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.pedirEliminar(null) }, enabled = !estado.guardando) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ResumenNutricion(
    titulo: String,
    valor: BigDecimal,
    unidad: String,
    cargando: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (cargando) "—" else "${formatearNumero(valor)} $unidad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun IngestaFila(
    ingesta: IngestaNutricionUi,
    editando: Boolean,
    cantidadEditada: String,
    guardando: Boolean,
    cambiarCantidad: (String) -> Unit,
    iniciarEdicion: () -> Unit,
    guardarEdicion: () -> Unit,
    cancelarEdicion: () -> Unit,
    eliminar: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ingesta.alimento, fontWeight = FontWeight.SemiBold)
                Text(
                    "${formatearHora(ingesta.fechaHora)} · ${formatearNumero(ingesta.cantidadG)} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("${formatearNumero(ingesta.kcal)} kcal", fontWeight = FontWeight.SemiBold)
        }
        Text(
            "${formatearNumero(ingesta.proteinaG)} g proteína · ${formatearNumero(ingesta.carbohidratoG)} g carb. · ${formatearNumero(ingesta.grasaG)} g grasa",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (editando) {
            OutlinedTextField(
                value = cantidadEditada,
                onValueChange = cambiarCantidad,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cantidad (g)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = !guardando
            )
            Row(modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = cancelarEdicion, enabled = !guardando) { Text("Cancelar") }
                TextButton(onClick = guardarEdicion, enabled = !guardando) { Text("Guardar") }
            }
        } else {
            Row(modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = iniciarEdicion, enabled = !guardando) { Text("Editar cantidad") }
                TextButton(onClick = eliminar, enabled = !guardando) { Text("Eliminar") }
            }
        }
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SelectorAlimentoDialog(
    alimentos: List<AlimentoNutricionResponse>,
    seleccionar: (Int) -> Unit,
    cerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Selecciona un alimento") },
        text = {
            if (alimentos.isEmpty()) Text("No hay alimentos que coincidan con la búsqueda.")
            else Column(Modifier.height(320.dp).verticalScroll(rememberScrollState())) {
                alimentos.forEach { alimento ->
                    val id = alimento.idAlimento
                    if (id != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth().clickable { seleccionar(id) }.padding(vertical = 10.dp)
                        ) {
                            Text(alimento.nombre.orEmpty(), fontWeight = FontWeight.SemiBold)
                            Text(
                                "Porción base ${formatearNumero(alimento.porcionBaseG ?: BigDecimal.ZERO)} g · ${formatearNumero(alimento.kcal ?: BigDecimal.ZERO)} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = cerrar) { Text("Cerrar") } }
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CalendarioNutricion(fecha: String, seleccionar: (String) -> Unit, cerrar: () -> Unit) {
    val limiteHoy = remember { Fechas.inicioDeHoyUtcMillis() }
    val estadoCalendario = rememberDatePickerState(
        initialSelectedDateMillis = Fechas.millisUtcDesdeIso(fecha),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= limiteHoy
        }
    )
    DatePickerDialog(
        onDismissRequest = cerrar,
        confirmButton = {
            TextButton(
                onClick = { estadoCalendario.selectedDateMillis?.let { seleccionar(Fechas.isoDesdeMillisUtc(it)) } },
                enabled = estadoCalendario.selectedDateMillis != null
            ) { Text("Ver día") }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } },
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        DatePicker(state = estadoCalendario)
    }
}

@Composable
private fun MensajeNutricion(texto: String, esError: Boolean, cerrar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                texto,
                modifier = Modifier.weight(1f),
                color = if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
            )
            TextButton(onClick = cerrar) { Text("×") }
        }
    }
}

private fun calcularNutrientes(
    alimento: AlimentoNutricionResponse,
    cantidad: BigDecimal?
): List<BigDecimal>? {
    val porcion = alimento.porcionBaseG?.takeIf { it > BigDecimal.ZERO } ?: return null
    val gramos = cantidad?.takeIf { it > BigDecimal.ZERO } ?: return null
    return listOf(alimento.kcal, alimento.proteinaG, alimento.carbohidratoG, alimento.grasaG)
        .map { it ?: BigDecimal.ZERO }
        .map { it.multiply(gramos).divide(porcion, 2, java.math.RoundingMode.HALF_UP) }
}

private fun formatearNumero(valor: BigDecimal): String = valor.stripTrailingZeros()
    .toPlainString().replace('.', ',')

private fun formatearFecha(iso: String): String {
    val fecha = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(iso)
    }.getOrNull() ?: return iso
    val localEspanol = Locale.forLanguageTag("es-PE")
    val texto = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", localEspanol).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(fecha)
    return texto.replaceFirstChar { it.uppercase(localEspanol) }
}

private fun formatearHora(fechaHora: String?): String {
    if (fechaHora == null || fechaHora.length < 16) return "Hora no disponible"
    return runCatching {
        val hora = fechaHora.substring(11, 16).split(':')
        val h24 = hora[0].toInt()
        val minuto = hora[1]
        val sufijo = if (h24 < 12) "a. m." else "p. m."
        "${if (h24 % 12 == 0) 12 else h24 % 12}:$minuto $sufijo"
    }.getOrDefault("Hora no disponible")
}

@Preview(showBackground = true)
@Composable
private fun NutricionPreview() {
    YourPeakTheme {
        Surface(Modifier.padding(16.dp)) {
            Column(Modifier.widthIn(max = 520.dp)) {
                EncabezadoTarjeta(
                    etiqueta = "CLIENTE",
                    titulo = "Nutrición",
                    introduccion = "Registra lo que comes y revisa el resumen de cada día."
                )
                Spacer(Modifier.height(12.dp))
                ResumenNutricion("Calorías", BigDecimal("1250"), "kcal", false, Modifier.fillMaxWidth())
            }
        }
    }
}
