package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.ramirez.yourpeak.ui.components.CarbonCampo
import com.ramirez.yourpeak.ui.components.TarjetaCarbon
import com.ramirez.yourpeak.ui.components.TextoSecundario
import com.ramirez.yourpeak.ui.theme.BarlowCondensed
import com.ramirez.yourpeak.ui.theme.CarbonBorde
import com.ramirez.yourpeak.ui.theme.Lima
import com.ramirez.yourpeak.ui.theme.LimaFondo
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

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ResumenSemana(estado)
        when {
            estado.cargando -> Row(
                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("  Cargando tu disponibilidad…", color = TextoSecundario, fontSize = 14.sp)
            }
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
                            mostrarSelectorHora(context, dia.horaInicio) { viewModel.cambiarHora(dia.numero, true, it) }
                        },
                        elegirHoraFin = {
                            mostrarSelectorHora(context, dia.horaFin) { viewModel.cambiarHora(dia.numero, false, it) }
                        }
                    )
                }
                estado.error?.let { MensajeDisponibilidad(it, esError = true) }
                estado.mensaje?.let { MensajeDisponibilidad(it, esError = false) { viewModel.cerrarMensaje() } }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = viewModel::restablecer,
                        enabled = estado.hayCambios && !estado.guardando,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CarbonBorde)
                    ) {
                        Text("RESTABLECER", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = viewModel::guardar,
                        enabled = estado.hayCambios && !estado.guardando,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (estado.guardando) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (estado.guardando) "GUARDANDO" else "GUARDAR", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

/** Días y horas por semana, con la inicial de cada día marcada si está disponible. */
@Composable
private fun ResumenSemana(estado: DisponibilidadUiState) {
    val minutos = estado.minutosSemanales
    val dias = estado.cantidadDiasDisponibles
    TarjetaCarbon {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tu semana", color = TextoSecundario, fontSize = 12.sp)
                Text(
                    "$dias ${if (dias == 1) "día" else "días"} · ${duracion(minutos)}",
                    color = Color.White,
                    fontFamily = BarlowCondensed,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    maxLines = 1
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                estado.dias.forEach { dia ->
                    Surface(
                        modifier = Modifier.size(22.dp),
                        shape = CircleShape,
                        color = if (dia.disponible) LimaFondo else Color.Transparent,
                        border = BorderStroke(1.dp, if (dia.disponible) Lima else CarbonBorde)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                listOf("L", "M", "X", "J", "V", "S", "D").getOrElse(dia.numero - 1) { "" },
                                fontSize = 10.sp,
                                color = if (dia.disponible) Lima else TextoSecundario
                            )
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
    TarjetaCarbon(relleno = 0.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = if (dia.disponible) 12.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dia.nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dia.disponible) Color.White else TextoSecundario,
                        modifier = Modifier.weight(1f)
                    )
                    if (!dia.disponible) Text("Descanso", fontSize = 12.sp, color = TextoSecundario)
                }
                if (dia.disponible) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChipHora(dia.horaInicio ?: "--:--", elegirHoraInicio)
                        Text("  →  ", color = TextoSecundario, fontSize = 14.sp)
                        ChipHora(dia.horaFin ?: "--:--", elegirHoraFin)
                        minutosDia(dia)?.let {
                            Text("  ${duracion(it)}", color = TextoSecundario, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = dia.disponible,
                onCheckedChange = cambiarDisponible,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color(0xFF8A909A),
                    uncheckedTrackColor = Color(0xFF2A2D33),
                    uncheckedBorderColor = Color(0xFF2A2D33)
                )
            )
        }
    }
}

/** Hora tocable que abre el selector del sistema. */
@Composable
private fun ChipHora(hora: String, alPulsar: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = alPulsar),
        shape = RoundedCornerShape(10.dp),
        color = CarbonCampo,
        border = BorderStroke(1.dp, CarbonBorde)
    ) {
        Text(
            hora,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
            color = Color.White,
            fontFamily = BarlowCondensed,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun MensajeDisponibilidad(mensaje: String, esError: Boolean, cerrar: (() -> Unit)? = null) {
    val acento = if (esError) Color(0xFFFF6B6B) else Lima
    TarjetaCarbon(borde = acento.copy(alpha = 0.55f), relleno = 4.dp) {
        Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = acento, fontSize = 14.sp)
            Text(mensaje, modifier = Modifier.weight(1f).padding(horizontal = 9.dp, vertical = 8.dp), color = Color.White, fontSize = 14.sp, lineHeight = 18.sp)
            cerrar?.let { TextButtonReintentar(onClick = it, texto = "Cerrar") }
        }
    }
}

@Composable
private fun TextButtonReintentar(onClick: () -> Unit, texto: String = "Reintentar") {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(texto, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary) }
}

private fun minutosDia(dia: DiaDisponibilidadUi): Int? {
    fun minutos(hora: String?): Int? = hora?.take(5)?.split(":")?.let { p ->
        p.getOrNull(0)?.toIntOrNull()?.let { h -> p.getOrNull(1)?.toIntOrNull()?.let { m -> h * 60 + m } }
    }
    val inicio = minutos(dia.horaInicio) ?: return null
    val fin = minutos(dia.horaFin) ?: return null
    return (fin - inicio).takeIf { it > 0 }
}

private fun duracion(minutos: Int): String = when {
    minutos % 60 == 0 -> "${minutos / 60} h"
    minutos < 60 -> "$minutos min"
    else -> "${minutos / 60} h ${minutos % 60} min"
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
