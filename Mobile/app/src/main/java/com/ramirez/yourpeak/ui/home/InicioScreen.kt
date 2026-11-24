package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.repository.NutricionRepository
import com.ramirez.yourpeak.data.repository.ProgresoRepository
import com.ramirez.yourpeak.data.repository.RutinaRepository
import com.ramirez.yourpeak.domain.Fechas
import java.math.BigDecimal
import java.text.DecimalFormat

private val FondoTarjetaInicio = Color(0xFF101010)
private val BordeTarjetaInicio = Color(0xFF303030)
private val TextoSecundarioInicio = Color(0xFFB8BCC6)

@Composable
fun InicioScreen(
    idUsuario: Long?,
    rutinaRepository: RutinaRepository,
    nutricionRepository: NutricionRepository,
    progresoRepository: ProgresoRepository,
    alIrARutina: () -> Unit,
    alIrANutricion: () -> Unit,
    alIrAProgreso: () -> Unit,
    viewModel: InicioViewModel = viewModel(
        factory = InicioViewModel.factory(rutinaRepository, nutricionRepository, progresoRepository, idUsuario)
    )
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val proximo = viewModel.proximoEntrenamiento()

    // Refresca los resúmenes cada vez que el usuario vuelve a Inicio.
    LaunchedEffect(idUsuario) {
        viewModel.actualizar()
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            viewModel.fechaAmigable(Fechas.hoyIso()),
            color = TextoSecundarioInicio,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 1.dp, bottom = 1.dp)
        )
        EntrenamientoInicio(
            estado = estado,
            proximo = proximo,
            viewModel = viewModel,
            alIrARutina = alIrARutina
        )
        ResumenNutricionalInicio(
            estado = estado,
            alReintentar = viewModel::actualizar,
            alIrANutricion = alIrANutricion
        )
        ActividadRecienteInicio(
            estado = estado,
            viewModel = viewModel,
            alIrARutina = alIrARutina
        )
        ProgresoInicio(
            estado = estado,
            viewModel = viewModel,
            alIrAProgreso = alIrAProgreso
        )
    }
}

@Composable
private fun EntrenamientoInicio(
    estado: InicioUiState,
    proximo: ProximoEntrenamientoUi?,
    viewModel: InicioViewModel,
    alIrARutina: () -> Unit
) {
    TarjetaInicio("PRÓXIMO ENTRENAMIENTO") {
        Spacer(Modifier.height(2.dp))
        Text(
            text = proximo?.dia?.ejercicios.orEmpty().mapNotNull { it.grupoMuscular?.takeIf(String::isNotBlank) }
                .distinct().take(2).joinToString(" + ").ifBlank { "MI RUTINA" }.uppercase(),
            color = Color.White,
            fontSize = 23.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val detalle = when {
            estado.rutinaCargando -> "Cargando tu rutina…"
            estado.rutinaError != null -> estado.rutinaError
            proximo == null -> "Aún no tienes ejercicios programados."
            proximo.diasHasta == 0 -> "Hoy · ${proximo.dia.horaInicio.orEmpty().take(5)} a ${proximo.dia.horaFin.orEmpty().take(5)}"
            else -> "${viewModel.nombreDia(proximo.dia.diaSemana ?: 1)} · ${proximo.dia.horaInicio.orEmpty().take(5)} a ${proximo.dia.horaFin.orEmpty().take(5)}"
        }
        Text(detalle, color = TextoSecundarioInicio, fontSize = 15.sp, lineHeight = 20.sp)
        proximo?.let {
            Text(
                "${it.dia.ejercicios.orEmpty().size} ${if (it.dia.ejercicios.orEmpty().size == 1) "ejercicio" else "ejercicios"}",
                color = Color(0xFF8EB8FF), fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }
        AccionInicio("VER MI RUTINA", alIrARutina)
    }
}

@Composable
private fun ResumenNutricionalInicio(
    estado: InicioUiState,
    alReintentar: () -> Unit,
    alIrANutricion: () -> Unit
) {
    TarjetaInicio("RESUMEN NUTRICIONAL") {
        val meta = estado.nutricion?.meta?.kcal ?: BigDecimal.ZERO
        Text(
            if (meta > BigDecimal.ZERO) "Meta diaria: ${formatear(meta)} kcal" else "Meta diaria no asignada",
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
            color = TextoSecundarioInicio,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(14.dp))
        when {
            estado.nutricionCargando -> IndicadorInicio("Cargando tu resumen…")
            estado.nutricionError != null -> MensajeErrorInicio(estado.nutricionError, alReintentar)
            estado.nutricion != null -> {
                val resumen = estado.nutricion
                val meta = resumen.meta
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GraficoCalorias(
                        consumidas = resumen.kcal ?: BigDecimal.ZERO,
                        meta = meta?.kcal ?: BigDecimal.ZERO,
                        modifier = Modifier.size(86.dp)
                    )
                    Spacer(Modifier.size(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BarraMeta("Proteínas", resumen.proteinaG, meta?.proteinaG, Color(0xFF29C99A))
                        BarraMeta("Carbohidratos", resumen.carbohidratoG, meta?.carbohidratoG, Color(0xFF2878FF))
                        BarraMeta("Grasas", resumen.grasaG, meta?.grasaG, Color(0xFFFF8A3D))
                    }
                }
                AccionInicio("VER NUTRICIÓN", alIrANutricion)
            }
            else -> Unit
        }
    }
}

@Composable
private fun GraficoCalorias(consumidas: BigDecimal, meta: BigDecimal, modifier: Modifier = Modifier) {
    val progreso = if (meta > BigDecimal.ZERO) (consumidas.toFloat() / meta.toFloat()).coerceIn(0f, 1f) else 0f
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().padding(4.dp).size(78.dp)) {
            drawArc(Color(0xFF292929), -90f, 360f, false, style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round))
            drawArc(Color(0xFF2878FF), -90f, 360f * progreso, false, style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatear(consumidas), color = Color.White, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black)
            Text("kcal", color = TextoSecundarioInicio, fontSize = 13.sp)
        }
    }
}

@Composable
private fun BarraMeta(titulo: String, consumido: BigDecimal?, meta: BigDecimal?, color: Color) {
    val cantidad = consumido ?: BigDecimal.ZERO
    val objetivo = meta ?: BigDecimal.ZERO
    val progreso = if (objetivo > BigDecimal.ZERO) (cantidad.toFloat() / objetivo.toFloat()).coerceIn(0f, 1f) else 0f
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, color = TextoSecundarioInicio, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
            Text(
                "${formatear(cantidad)} / ${if (objetivo > BigDecimal.ZERO) formatear(objetivo) else "—"} g",
                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xFF292929), CircleShape)) {
            Box(Modifier.fillMaxWidth(progreso).height(5.dp).background(color, CircleShape))
        }
    }
}

@Composable
private fun ActividadRecienteInicio(estado: InicioUiState, viewModel: InicioViewModel, alIrARutina: () -> Unit) {
    TarjetaInicio("ACTIVIDAD RECIENTE") {
        Spacer(Modifier.height(2.dp))
        when {
            estado.rutinaCargando -> IndicadorInicio("Cargando actividad…")
            estado.rutinaError != null -> MensajeErrorInicio(estado.rutinaError) { viewModel.actualizar() }
            estado.rutina?.actividadReciente.isNullOrEmpty() -> Text(
                "Aún no hay ejercicios realizados registrados.",
                color = TextoSecundarioInicio, fontSize = 15.sp, lineHeight = 20.sp
            )
            else -> estado.rutina?.actividadReciente.orEmpty().take(2).forEach { actividad ->
                Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(actividad.titulo.orEmpty(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${viewModel.fechaAmigable(actividad.fecha.orEmpty())} · ${actividad.ejercicios ?: 0} ${if (actividad.ejercicios == 1) "ejercicio" else "ejercicios"}",
                        color = TextoSecundarioInicio, fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgresoInicio(estado: InicioUiState, viewModel: InicioViewModel, alIrAProgreso: () -> Unit) {
    TarjetaInicio("MI PROGRESO") {
        Spacer(Modifier.height(2.dp))
        when {
            estado.progresoCargando -> IndicadorInicio("Cargando tu progreso…")
            estado.progresoError != null -> MensajeErrorInicio(estado.progresoError) { viewModel.actualizar() }
            estado.ultimoPeso?.pesoKg != null -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatear(estado.ultimoPeso.pesoKg), color = Color.White, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black)
                    Text(" kg", color = TextoSecundarioInicio, fontSize = 14.sp, modifier = Modifier.padding(bottom = 3.dp))
                }
                Text("Último peso registrado · ${viewModel.fechaPesoAmigable(estado.ultimoPeso.fechaRegistro)}", color = TextoSecundarioInicio, fontSize = 14.sp)
            }
            else -> Text("Aún no registras tu peso.", color = TextoSecundarioInicio, fontSize = 14.sp)
        }
        AccionInicio("VER MI EVOLUCIÓN", alIrAProgreso)
    }
}

@Composable
private fun TarjetaInicio(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        TituloSeccionInicio(titulo)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = FondoTarjetaInicio,
            border = BorderStroke(1.dp, BordeTarjetaInicio)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), content = { contenido() })
        }
    }
}

@Composable
private fun TituloSeccionInicio(texto: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = texto,
            color = Color.White,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.35.sp,
            maxLines = 1
        )
        Spacer(Modifier.size(width = 8.dp, height = 1.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Color(0xFF292929)))
    }
}

@Composable
private fun AccionInicio(texto: String, accion: () -> Unit) {
    TextButton(onClick = accion, modifier = Modifier.padding(start = 0.dp, top = 2.dp)) {
        Text("$texto  →", color = Color(0xFF4B91FF), fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IndicadorInicio(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(Modifier.size(18.dp), color = Color(0xFF2878FF), strokeWidth = 2.dp)
        Text(texto, color = TextoSecundarioInicio, fontSize = 15.sp)
    }
}

@Composable
private fun MensajeErrorInicio(texto: String, reintentar: () -> Unit) {
    Column {
        Text(texto, color = Color(0xFFFF8D8D), fontSize = 15.sp, lineHeight = 20.sp)
        TextButton(onClick = reintentar) { Text("REINTENTAR", color = Color(0xFF4B91FF), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}

private fun formatear(valor: BigDecimal?): String {
    if (valor == null) return "0"
    return DecimalFormat("#,##0.#").format(valor)
}
