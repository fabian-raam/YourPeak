package com.ramirez.yourpeak.ui.home

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.repository.DisponibilidadRepository
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.theme.YourPeakTheme
import java.time.LocalTime
import java.util.Locale

@Composable
fun DisponibilidadScreen(idUsuario: Long?, repository: DisponibilidadRepository) {
    val viewModel: DisponibilidadViewModel = viewModel(
        key = "disponibilidad-$idUsuario",
        factory = DisponibilidadViewModel.factory(repository, idUsuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val minutos = estado.minutosSemanales
    val resumenHoras = "${minutos / 60} h ${minutos % 60} min por semana"

    Column {
        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Horarios semanales",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Elige un rango por cada día disponible.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "${estado.cantidadDiasDisponibles} días · $resumenHoras",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                when {
                    estado.cargando -> CircularProgressIndicator()
                    estado.error != null && estado.diasGuardados.all { !it.disponible } && estado.dias.all { !it.disponible } -> {
                        Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButtonReintentar(onClick = viewModel::reintentar)
                    }
                    else -> {
                        estado.dias.forEach { dia ->
                            DiaDisponibilidadFila(
                                dia = dia,
                                cambiarDisponible = { viewModel.cambiarDisponible(dia.numero, it) },
                                elegirHoraInicio = {
                                    mostrarSelectorHora(context, dia.horaInicio) {
                                        viewModel.cambiarHora(dia.numero, true, it)
                                    }
                                },
                                elegirHoraFin = {
                                    mostrarSelectorHora(context, dia.horaFin) {
                                        viewModel.cambiarHora(dia.numero, false, it)
                                    }
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        if (estado.error != null) {
                            MensajeDisponibilidad(estado.error.orEmpty(), esError = true)
                            Spacer(Modifier.height(10.dp))
                        }
                        estado.mensaje?.let { mensaje ->
                            MensajeDisponibilidad(mensaje, esError = false) {
                                viewModel.cerrarMensaje()
                            }
                            Spacer(Modifier.height(10.dp))
                        }

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
                                Text(if (estado.guardando) "Guardando" else "Guardar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaDisponibilidadFila(
    dia: DiaDisponibilidadUi,
    cambiarDisponible: (Boolean) -> Unit,
    elegirHoraInicio: () -> Unit,
    elegirHoraFin: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (dia.disponible) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(dia.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Switch(checked = dia.disponible, onCheckedChange = cambiarDisponible)
            }
            if (dia.disponible) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = elegirHoraInicio, modifier = Modifier.weight(1f)) {
                        Text("Desde\n${dia.horaInicio ?: "Elegir"}")
                    }
                    OutlinedButton(onClick = elegirHoraFin, modifier = Modifier.weight(1f)) {
                        Text("Hasta\n${dia.horaFin ?: "Elegir"}")
                    }
                }
            }
        }
    }
}

@Composable
private fun MensajeDisponibilidad(mensaje: String, esError: Boolean, cerrar: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = if (esError) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                mensaje,
                modifier = Modifier.weight(1f),
                color = if (esError) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodySmall
            )
            cerrar?.let {
                TextButtonReintentar(onClick = it, texto = "Cerrar")
            }
        }
    }
}

@Composable
private fun TextButtonReintentar(onClick: () -> Unit, texto: String = "Reintentar") {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(texto) }
}

private fun mostrarSelectorHora(
    context: android.content.Context,
    horaActual: String?,
    alElegir: (String) -> Unit
) {
    val partes = horaActual?.split(":")
    val hora = partes?.getOrNull(0)?.toIntOrNull() ?: 18
    val minuto = partes?.getOrNull(1)?.toIntOrNull() ?: 0
    TimePickerDialog(
        context,
        { _, horaElegida, minutoElegido ->
            alElegir(String.format(Locale.ROOT, "%02d:%02d", horaElegida, minutoElegido))
        },
        hora,
        minuto,
        true
    ).show()
}

@Preview(showBackground = true)
@Composable
private fun DisponibilidadPreview() {
    YourPeakTheme {
        Surface(Modifier.padding(16.dp)) {
            Column {
                EncabezadoTarjeta(
                    etiqueta = "ENTRENAMIENTO",
                    titulo = "Disponibilidad",
                    introduccion = "Indica los días y horarios en que puedes entrenar."
                )
                Spacer(Modifier.height(12.dp))
                DiaDisponibilidadFila(
                    dia = DiaDisponibilidadUi(1, "Lunes", true, "18:00", "20:00"),
                    cambiarDisponible = {},
                    elegirHoraInicio = {},
                    elegirHoraFin = {}
                )
            }
        }
    }
}
