package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.model.MembresiaPeriodoResponse
import com.ramirez.yourpeak.data.repository.MembresiaClienteRepository
import com.ramirez.yourpeak.ui.components.AnilloProgreso
import com.ramirez.yourpeak.ui.components.CarbonCampo
import com.ramirez.yourpeak.ui.components.CifraGrande
import com.ramirez.yourpeak.ui.components.EtiquetaEstado
import com.ramirez.yourpeak.ui.components.TarjetaCarbon
import com.ramirez.yourpeak.ui.components.TextoSecundario
import com.ramirez.yourpeak.ui.components.TituloSeccion
import com.ramirez.yourpeak.ui.theme.AzulDestacadoFin
import com.ramirez.yourpeak.ui.theme.AzulDestacadoInicio
import com.ramirez.yourpeak.ui.theme.BarlowCondensed
import com.ramirez.yourpeak.ui.theme.CarbonBorde
import com.ramirez.yourpeak.ui.theme.Lima
import com.ramirez.yourpeak.ui.theme.LimaFondo
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RojoMembresia = Color(0xFFFF6B6B)
private val es = Locale.forLanguageTag("es-PE")

@Composable
fun MiMembresiaScreen(usuario: LoginResponse, repository: MembresiaClienteRepository) {
    val viewModel: MiMembresiaViewModel = viewModel(
        key = "mi-membresia-${usuario.idUsuario}",
        factory = MiMembresiaViewModel.factory(repository, usuario.idUsuario, usuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val actual = estado.actual

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TarjetaPlan(estado, actual)

        if (actual != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TarjetaCarbon(Modifier.weight(1f)) {
                    Text("Inicio", color = TextoSecundario, fontSize = 12.sp)
                    CifraGrande(formatoCorto(actual.fechaInicio), tamano = 24)
                }
                TarjetaCarbon(Modifier.weight(1f)) {
                    Text("Vencimiento", color = TextoSecundario, fontSize = 12.sp)
                    CifraGrande(formatoCorto(actual.fechaVencimiento), tamano = 24)
                }
            }
        }

        estado.error?.let { error ->
            Column {
                Text(error, color = RojoMembresia, fontSize = 14.sp, lineHeight = 19.sp)
                TextButton(onClick = viewModel::actualizar, enabled = !estado.cargando) {
                    Text("REINTENTAR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        TarjetaCarbon {
            Text(
                "Para renovar o cambiar de plan, acércate a la recepción del gimnasio.",
                color = TextoSecundario,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        if (estado.periodos.isNotEmpty()) {
            TituloSeccion("HISTORIAL", Modifier.padding(top = 6.dp))
            TarjetaCarbon(relleno = 0.dp) {
                estado.periodos.forEachIndexed { indice, periodo ->
                    if (indice > 0) HorizontalDivider(color = CarbonBorde, modifier = Modifier.padding(horizontal = 14.dp))
                    FilaPeriodo(periodo)
                }
            }
        }
    }
}

/** Tarjeta azul con el plan actual, su estado y un anillo de días restantes. */
@Composable
private fun TarjetaPlan(estado: MiMembresiaUiState, actual: MembresiaPeriodoResponse?) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(AzulDestacadoInicio, AzulDestacadoFin)))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            val (texto, color) = when {
                estado.cargando && actual == null && estado.membresiaVigente == null -> "CONSULTANDO…" to Color.White
                actual?.estadoPeriodo.equals("PROGRAMADA", true) -> "● PROGRAMADA" to Color.White
                actual != null || estado.membresiaVigente == true -> "● VIGENTE" to Lima
                else -> "● NO VIGENTE" to RojoMembresia
            }
            EtiquetaEstado(texto, color, Color.Black.copy(alpha = 0.25f))
            Spacer(Modifier.height(10.dp))
            Text(
                (actual?.nombrePlan ?: if (estado.membresiaVigente == false) "SIN PLAN ACTIVO" else "MI PLAN").uppercase(),
                color = Color.White,
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                lineHeight = 34.sp
            )
            Text(
                actual?.fechaVencimiento?.let { "Vence el ${formatoLargo(it)}" }
                    ?: if (estado.membresiaVigente == false) "Comunícate con la administración para renovarla." else "",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
        }
        val dias = estado.diasRestantes()
        if (dias != null) {
            Spacer(Modifier.width(10.dp))
            AnilloProgreso(
                progreso = dias.first.toFloat() / dias.second,
                centro = dias.first.toString(),
                leyenda = if (dias.first == 1) "día restante" else "días restantes",
                color = Color.White,
                pista = Color.White.copy(alpha = 0.2f),
                colorLeyenda = Color.White.copy(alpha = 0.8f)
            )
        } else if (estado.cargando) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun FilaPeriodo(periodo: MembresiaPeriodoResponse) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(periodo.nombrePlan ?: "Plan", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${formatoCorto(periodo.fechaInicio)} – ${formatoCorto(periodo.fechaVencimiento, conAnio = true)}",
                color = TextoSecundario,
                fontSize = 12.sp
            )
        }
        when (periodo.estadoPeriodo?.uppercase()) {
            "VIGENTE" -> EtiquetaEstado("Vigente", Lima, LimaFondo)
            "PROGRAMADA" -> EtiquetaEstado("Programada", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            else -> EtiquetaEstado("Finalizada", TextoSecundario, CarbonCampo)
        }
    }
}

private fun formatoCorto(iso: String?, conAnio: Boolean = false): String =
    fechaIso(iso)?.format(DateTimeFormatter.ofPattern(if (conAnio) "d MMM yyyy" else "d MMM", es)) ?: "—"

private fun formatoLargo(iso: String): String =
    fechaIso(iso)?.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", es)) ?: iso
