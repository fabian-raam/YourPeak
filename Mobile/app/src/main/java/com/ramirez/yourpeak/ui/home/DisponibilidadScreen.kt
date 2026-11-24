package com.ramirez.yourpeak.ui.home

import android.app.TimePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.repository.DisponibilidadRepository
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.theme.YourPeakTheme
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

    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Text(
            "${estado.cantidadDiasDisponibles} ${if (estado.cantidadDiasDisponibles == 1) "día" else "días"} · $resumenHoras",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFB8BCC6)
        )
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFF292929))

        when {
            estado.cargando -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            estado.error != null && estado.diasGuardados.all { !it.disponible } && estado.dias.all { !it.disponible } -> {
                MensajeDisponibilidad(estado.error.orEmpty(), esError = true)
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
                }

                if (estado.error != null) {
                    Spacer(Modifier.height(10.dp))
                    MensajeDisponibilidad(estado.error.orEmpty(), esError = true)
                }
                estado.mensaje?.let { mensaje ->
                    Spacer(Modifier.height(10.dp))
                    MensajeDisponibilidad(mensaje, esError = false) {
                        viewModel.cerrarMensaje()
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::restablecer,
                        enabled = estado.hayCambios && !estado.guardando,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        Text("RESTABLECER", fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = viewModel::guardar,
                        enabled = estado.hayCambios && !estado.guardando,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (estado.guardando) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (estado.guardando) "GUARDANDO" else "GUARDAR",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
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
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                dia.nombre,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Switch(
                checked = dia.disponible,
                onCheckedChange = cambiarDisponible,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFB7F34A),
                    checkedBorderColor = Color(0xFFB7F34A),
                    uncheckedThumbColor = Color(0xFFF0F0F0),
                    uncheckedTrackColor = Color(0xFF454545),
                    uncheckedBorderColor = Color(0xFF454545)
                )
            )
        }
        if (dia.disponible) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilaHoraDisponibilidad("Desde", dia.horaInicio ?: "Elegir hora", elegirHoraInicio)
                FilaHoraDisponibilidad("Hasta", dia.horaFin ?: "Elegir hora", elegirHoraFin)
            }
            Spacer(Modifier.height(8.dp))
        }
        HorizontalDivider(color = Color(0xFF292929))
    }
}

@Composable
private fun FilaHoraDisponibilidad(etiqueta: String, hora: String, alPulsar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = alPulsar),
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF191919)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(etiqueta, fontSize = 16.sp, color = Color(0xFFB8BCC6))
            Text(hora, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
private fun MensajeDisponibilidad(mensaje: String, esError: Boolean, cerrar: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (esError) Color(0xFF241516) else Color(0xFF11130B),
        border = BorderStroke(1.dp, if (esError) Color(0xFF713B3E) else Color(0xFF46551A))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                mensaje,
                modifier = Modifier.weight(1f),
                color = if (esError) Color(0xFFFFB4B4) else Color.White,
                fontSize = 15.sp,
                lineHeight = 18.sp
            )
            cerrar?.let {
                TextButtonReintentar(onClick = it, texto = "Cerrar")
            }
        }
    }
}

@Composable
private fun TextButtonReintentar(onClick: () -> Unit, texto: String = "Reintentar") {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(texto, fontSize = 15.sp) }
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
        Surface(Modifier.padding(16.dp), color = Color.Black) {
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
