package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ramirez.yourpeak.data.model.EjercicioResponse
import com.ramirez.yourpeak.data.repository.EjercicioRepository
import com.ramirez.yourpeak.data.repository.RutinaRepository
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.theme.YourPeakTheme

@Composable
fun MiRutinaScreen(
    idUsuario: Long?,
    rutinaRepository: RutinaRepository,
    ejercicioRepository: EjercicioRepository,
    alIrADisponibilidad: () -> Unit
) {
    val viewModel: MiRutinaViewModel = viewModel(
        key = "mi-rutina-$idUsuario",
        factory = MiRutinaViewModel.factory(
            rutinaRepository, ejercicioRepository, idUsuario
        )
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dia = estado.diaActual
    var mostrarSelector by remember { mutableStateOf(false) }

    Column {
        EncabezadoTarjeta(
            etiqueta = "ENTRENAMIENTO",
            titulo = "Mi rutina",
            introduccion = "Organiza tus ejercicios en los días que puedes entrenar."
        )
        Spacer(Modifier.height(14.dp))

        when {
            estado.cargando -> CircularProgressIndicator()
            estado.error != null && estado.diasGuardados.none { it.disponible } && estado.dias.none { it.disponible } -> {
                MensajeRutina(estado.error.orEmpty(), error = true)
                TextButton(onClick = viewModel::reintentar) { Text("Reintentar") }
            }
            else -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Semana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            estado.dias.forEach { item ->
                                FilterChip(
                                    selected = estado.diaSeleccionado == item.numero,
                                    onClick = { viewModel.seleccionarDia(item.numero) },
                                    label = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(item.nombre.take(3))
                                            Text(
                                                if (item.disponible) "Disponible" else "Descanso",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (dia != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                dia.nombre.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (dia.disponible) {
                                    "Disponible · ${dia.horaInicio ?: "--:--"} a ${dia.horaFin ?: "--:--"}"
                                } else "Día de descanso · no está marcado como disponible",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))

                            if (!dia.disponible) {
                                Text(
                                    "Para asignar ejercicios a este día, primero configura tu disponibilidad.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = alIrADisponibilidad) {
                                    Text("Configurar disponibilidad")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Ejercicios del día",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text("${dia.ejercicios.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(8.dp))
                                if (dia.ejercicios.isEmpty()) {
                                    Text(
                                        "Todavía no agregaste ejercicios para este día.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    dia.ejercicios.forEach { ejercicio ->
                                        EjercicioAsignadoFila(
                                            ejercicio = ejercicio,
                                            quitar = { viewModel.quitarEjercicio(ejercicio.idEjercicio) }
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { mostrarSelector = true },
                                    enabled = estado.catalogo.isNotEmpty()
                                ) { Text("Agregar ejercicio") }

                                val ejercicioElegido = estado.catalogo.firstOrNull {
                                    it.idEjercicio == estado.idEjercicioSeleccionado
                                }
                                if (ejercicioElegido != null) {
                                    Spacer(Modifier.height(12.dp))
                                    FormularioEjercicioRutina(
                                        ejercicio = ejercicioElegido,
                                        series = estado.series,
                                        valorMedicion = estado.valorMedicion,
                                        descanso = estado.descanso,
                                        cambiarSeries = viewModel::cambiarSeries,
                                        cambiarValorMedicion = viewModel::cambiarValorMedicion,
                                        cambiarDescanso = viewModel::cambiarDescanso,
                                        agregar = viewModel::agregarEjercicio,
                                        cancelar = { viewModel.seleccionarEjercicio(null) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (estado.error != null) {
                    Spacer(Modifier.height(10.dp))
                    MensajeRutina(estado.error.orEmpty(), error = true)
                }
                estado.mensaje?.let {
                    Spacer(Modifier.height(10.dp))
                    MensajeRutina(it, error = false) { viewModel.cerrarMensaje() }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = viewModel::restablecer,
                        enabled = estado.hayCambios && !estado.guardando
                    ) { Text("Restablecer") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = viewModel::guardar,
                        enabled = estado.hayCambios && !estado.guardando
                    ) {
                        if (estado.guardando) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (estado.guardando) "Guardando" else "Guardar rutina")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "La rutina se repite cada semana.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (mostrarSelector && dia != null) {
        SelectorEjercicioDialog(
            ejercicios = estado.catalogo.filterNot { ejercicio ->
                dia.ejercicios.any { it.idEjercicio == ejercicio.idEjercicio }
            },
            seleccionar = { id ->
                viewModel.seleccionarEjercicio(id)
                mostrarSelector = false
            },
            cerrar = { mostrarSelector = false }
        )
    }
}

@Composable
private fun EjercicioAsignadoFila(ejercicio: EjercicioRutinaUi, quitar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(ejercicio.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        ejercicio.grupoMuscular,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = quitar) { Text("Quitar") }
            }
            val medicion = if (ejercicio.tipoMedicion.equals("TIEMPO", ignoreCase = true)) {
                "${ejercicio.segundos ?: 0} s"
            } else "${ejercicio.repeticiones ?: 0} rep."
            Text(
                "${ejercicio.series} series × $medicion · descanso ${ejercicio.descansoSegundos} s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FormularioEjercicioRutina(
    ejercicio: EjercicioResponse,
    series: String,
    valorMedicion: String,
    descanso: String,
    cambiarSeries: (String) -> Unit,
    cambiarValorMedicion: (String) -> Unit,
    cambiarDescanso: (String) -> Unit,
    agregar: () -> Unit,
    cancelar: () -> Unit
) {
    val esTiempo = ejercicio.tipoMedicion.equals("TIEMPO", ignoreCase = true)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Agregar: ${ejercicio.nombre.orEmpty()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoNumero(
                    valor = series,
                    etiqueta = "Series",
                    cambiar = cambiarSeries,
                    modifier = Modifier.weight(1f)
                )
                CampoNumero(
                    valor = valorMedicion,
                    etiqueta = if (esTiempo) "Segundos" else "Repeticiones",
                    cambiar = cambiarValorMedicion,
                    modifier = Modifier.weight(1.4f)
                )
            }
            Spacer(Modifier.height(8.dp))
            CampoNumero(
                valor = descanso,
                etiqueta = "Descanso (s)",
                cambiar = cambiarDescanso,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = cancelar) { Text("Cancelar") }
                Button(onClick = agregar) { Text("Agregar al día") }
            }
        }
    }
}

@Composable
private fun CampoNumero(
    valor: String,
    etiqueta: String,
    cambiar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = cambiar,
        modifier = modifier,
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun SelectorEjercicioDialog(
    ejercicios: List<EjercicioResponse>,
    seleccionar: (Int?) -> Unit,
    cerrar: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    val filtrados = ejercicios.filter { ejercicio ->
        listOfNotNull(ejercicio.nombre, ejercicio.grupoMuscular)
            .any { it.contains(busqueda, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Elige un ejercicio") },
        text = {
            Column {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Buscar") }
                )
                Spacer(Modifier.height(8.dp))
                if (filtrados.isEmpty()) {
                    Text("No hay ejercicios disponibles que coincidan.")
                } else {
                    Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                        filtrados.forEach { ejercicio ->
                            val id = ejercicio.idEjercicio
                            if (id != null) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { seleccionar(id) },
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(ejercicio.nombre.orEmpty(), fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${ejercicio.grupoMuscular.orEmpty()} · ${ejercicio.tipoMedicion.orEmpty()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = cerrar) { Text("Cerrar") } }
    )
}

@Composable
private fun MensajeRutina(mensaje: String, error: Boolean, cerrar: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = if (error) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                mensaje,
                modifier = Modifier.weight(1f),
                color = if (error) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodySmall
            )
            cerrar?.let { TextButton(onClick = it) { Text("Cerrar") } }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MiRutinaPreview() {
    YourPeakTheme {
        Surface(Modifier.padding(16.dp)) {
            Column {
                EncabezadoTarjeta(
                    etiqueta = "ENTRENAMIENTO",
                    titulo = "Mi rutina",
                    introduccion = "Organiza tus ejercicios en los días que puedes entrenar."
                )
                Spacer(Modifier.height(12.dp))
                EjercicioAsignadoFila(
                    EjercicioRutinaUi(1, "Press de banca plano", "Pecho", "REPETICIONES", 4, 8, null, 90),
                    quitar = {}
                )
            }
        }
    }
}
