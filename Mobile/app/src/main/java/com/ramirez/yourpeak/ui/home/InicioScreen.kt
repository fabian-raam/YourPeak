package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.ramirez.yourpeak.ui.components.IlustracionEjercicio
import com.ramirez.yourpeak.ui.theme.AzulDestacadoFin
import com.ramirez.yourpeak.ui.theme.AzulDestacadoInicio
import com.ramirez.yourpeak.ui.theme.BarlowCondensed
import com.ramirez.yourpeak.ui.theme.Carbon
import com.ramirez.yourpeak.ui.theme.CarbonBorde
import com.ramirez.yourpeak.ui.theme.Lima
import com.ramirez.yourpeak.ui.theme.LimaFondo
import java.math.BigDecimal
import java.text.DecimalFormat

private val TextoSecundarioInicio = Color(0xFF9AA1AD)
private val ColorProteina = Color(0xFF2F7BFF)
private val ColorCarbohidrato = Color(0xFFFF8A3D)
private val ColorGrasa = Lima

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
        Text(viewModel.fechaAmigable(Fechas.hoyIso()), color = TextoSecundarioInicio, fontSize = 14.sp)
        if (!estado.rutinaCargando && estado.rutinaError == null) {
            SemanaInicio(viewModel.semanaActual())
        }
        TarjetaProximoEntrenamiento(estado, proximo, viewModel, alIrARutina)
        TituloSeccionInicio("NUTRICIÓN DE HOY")
        ResumenNutricionalInicio(estado, viewModel::actualizar, alIrANutricion)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaPeso(estado, viewModel, alIrAProgreso, Modifier.weight(1f))
            TarjetaSemana(viewModel.semanaActual(), Modifier.weight(1f))
        }
        ActividadRecienteInicio(estado, viewModel)
    }
}

/** Tira lunes–domingo: lima = entrenado, azul = hoy, atenuado = día de descanso. */
@Composable
private fun SemanaInicio(dias: List<DiaSemanaInicioUi>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        dias.forEach { dia ->
            val fondo = when {
                dia.esHoy -> MaterialTheme.colorScheme.primary
                dia.entrenado -> LimaFondo
                else -> Carbon
            }
            val borde = when {
                dia.esHoy -> MaterialTheme.colorScheme.primary
                dia.entrenado -> Lima
                else -> CarbonBorde
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = fondo,
                border = BorderStroke(1.dp, borde)
            ) {
                Column(
                    Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        dia.letra,
                        fontSize = 11.sp,
                        color = if (dia.esHoy) Color.White else TextoSecundarioInicio
                    )
                    Text(
                        dia.numero.toString(),
                        fontFamily = BarlowCondensed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = when {
                            dia.esHoy || dia.entrenado -> Color.White
                            dia.disponible -> Color.White
                            else -> Color(0xFF6B717C)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaProximoEntrenamiento(
    estado: InicioUiState,
    proximo: ProximoEntrenamientoUi?,
    viewModel: InicioViewModel,
    alIrARutina: () -> Unit
) {
    val ejercicios = proximo?.dia?.ejercicios.orEmpty()
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 176.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(AzulDestacadoInicio, AzulDestacadoFin)))
    ) {
        ejercicios.firstOrNull()?.let { primero ->
            IlustracionEjercicio(
                imagenRuta = primero.imagenRuta,
                grupoMuscular = primero.grupoMuscular,
                forma = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 14.dp)
                    .width(132.dp)
                    .height(88.dp)
            )
        }
        Column(Modifier.padding(18.dp).fillMaxWidth(if (ejercicios.isEmpty()) 1f else 0.62f)) {
            val cuando = when {
                estado.rutinaCargando -> "CARGANDO…"
                proximo == null -> "PRÓXIMO ENTRENAMIENTO"
                proximo.diasHasta == 0 -> "HOY · ${proximo.dia.horaInicio.orEmpty().take(5)}"
                else -> "${viewModel.nombreDia(proximo.dia.diaSemana ?: 1).uppercase()} · ${proximo.dia.horaInicio.orEmpty().take(5)}"
            }
            Text(cuando, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(6.dp))
            val titulo = ejercicios.mapNotNull { it.grupoMuscular?.takeIf(String::isNotBlank) }
                .distinct().take(2).joinToString(" + ").ifBlank { "MI RUTINA" }.uppercase()
            Text(
                titulo,
                color = Color.White,
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                lineHeight = 32.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val detalle = when {
                estado.rutinaError != null -> estado.rutinaError
                proximo == null -> "Genera tu rutina con IA o agrega ejercicios."
                else -> {
                    val minutos = minutosEntre(proximo.dia.horaInicio, proximo.dia.horaFin)
                    "${ejercicios.size} ${if (ejercicios.size == 1) "ejercicio" else "ejercicios"}" +
                        (minutos?.let { " · $it min" } ?: "")
                }
            }
            Text(detalle, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = alIrARutina,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AzulDestacadoFin)
            ) {
                Text(if (proximo == null) "IR A MI RUTINA" else "VER RUTINA", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
            }
        }
    }
}

@Composable
private fun ResumenNutricionalInicio(
    estado: InicioUiState,
    alReintentar: () -> Unit,
    alIrANutricion: () -> Unit
) {
    TarjetaCarbon {
        when {
            estado.nutricionCargando -> IndicadorInicio("Cargando tu resumen…")
            estado.nutricionError != null -> MensajeErrorInicio(estado.nutricionError, alReintentar)
            estado.nutricion != null -> {
                val resumen = estado.nutricion
                val meta = resumen.meta
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnilloCalorias(
                        consumidas = resumen.kcal ?: BigDecimal.ZERO,
                        meta = meta?.kcal ?: BigDecimal.ZERO,
                        modifier = Modifier.size(104.dp)
                    )
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        BarraMeta("Proteínas", resumen.proteinaG, meta?.proteinaG, ColorProteina)
                        BarraMeta("Carbohidratos", resumen.carbohidratoG, meta?.carbohidratoG, ColorCarbohidrato)
                        BarraMeta("Grasas", resumen.grasaG, meta?.grasaG, ColorGrasa)
                    }
                }
                AccionInicio("VER NUTRICIÓN", alIrANutricion)
            }
            else -> Unit
        }
    }
}

/** Muestra las kcal que faltan para la meta; si no hay meta, las consumidas. */
@Composable
private fun AnilloCalorias(consumidas: BigDecimal, meta: BigDecimal, modifier: Modifier = Modifier) {
    val hayMeta = meta > BigDecimal.ZERO
    val progreso = if (hayMeta) (consumidas.toFloat() / meta.toFloat()).coerceIn(0f, 1f) else 0f
    val primario = MaterialTheme.colorScheme.primary
    val metaCumplida = hayMeta && consumidas >= meta
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(96.dp)) {
            drawArc(CarbonBorde, -90f, 360f, false, style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round))
            drawArc(if (metaCumplida) Lima else primario, -90f, 360f * progreso, false, style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val valor = if (hayMeta) (meta - consumidas).max(BigDecimal.ZERO) else consumidas
            Text(
                formatear(valor.setScale(0, java.math.RoundingMode.HALF_UP)),
                color = Color.White, fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 24.sp
            )
            Text(if (hayMeta) "kcal restantes" else "kcal", color = TextoSecundarioInicio, fontSize = 10.sp)
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
            Text(titulo, color = TextoSecundarioInicio, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
            Text(
                "${formatear(cantidad)} / ${if (objetivo > BigDecimal.ZERO) formatear(objetivo) else "—"} g",
                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().height(7.dp).background(CarbonBorde, CircleShape)) {
            Box(Modifier.fillMaxWidth(progreso).height(7.dp).background(color, CircleShape))
        }
    }
}

@Composable
private fun TarjetaPeso(estado: InicioUiState, viewModel: InicioViewModel, alIrAProgreso: () -> Unit, modifier: Modifier) {
    TarjetaCarbon(modifier.clickable(onClick = alIrAProgreso)) {
        Text("Peso actual", color = TextoSecundarioInicio, fontSize = 12.sp)
        when {
            estado.progresoCargando -> IndicadorInicio("Cargando…")
            estado.ultimoPeso?.pesoKg != null -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatear(estado.ultimoPeso.pesoKg), color = Color.White, fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
                    Text(" kg", color = TextoSecundarioInicio, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                Text(viewModel.fechaPesoAmigable(estado.ultimoPeso.fechaRegistro), color = TextoSecundarioInicio, fontSize = 12.sp)
            }
            else -> Text("Aún no registras tu peso.", color = TextoSecundarioInicio, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** Entrenamientos con actividad registrada esta semana frente a los días disponibles. */
@Composable
private fun TarjetaSemana(dias: List<DiaSemanaInicioUi>, modifier: Modifier) {
    val entrenados = dias.count { it.entrenado }
    val planeados = dias.count { it.disponible }
    TarjetaCarbon(modifier) {
        Text("Esta semana", color = TextoSecundarioInicio, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(entrenados.toString(), color = Color.White, fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
            Text(" / $planeados entrenos", color = TextoSecundarioInicio, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text(
            when {
                planeados == 0 -> "Configura tu disponibilidad."
                entrenados >= planeados -> "¡Semana completa!"
                entrenados > 0 -> "¡Sigue así!"
                else -> "Tu semana empieza hoy."
            },
            color = if (entrenados > 0) Lima else TextoSecundarioInicio,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ActividadRecienteInicio(estado: InicioUiState, viewModel: InicioViewModel) {
    val actividades = estado.rutina?.actividadReciente.orEmpty()
    if (estado.rutinaCargando || estado.rutinaError != null || actividades.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSeccionInicio("ACTIVIDAD RECIENTE")
        TarjetaCarbon {
            actividades.take(3).forEachIndexed { indice, actividad ->
                if (indice > 0) Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Lima))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(actividad.titulo.orEmpty(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${viewModel.fechaAmigable(actividad.fecha.orEmpty().take(10))} · ${actividad.ejercicios ?: 0} ${if (actividad.ejercicios == 1) "ejercicio" else "ejercicios"}",
                            color = TextoSecundarioInicio, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaCarbon(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Carbon,
        border = BorderStroke(1.dp, CarbonBorde)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) { contenido() }
    }
}

@Composable
private fun TituloSeccionInicio(texto: String) {
    Text(
        text = texto,
        color = Color.White,
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 19.sp,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun AccionInicio(texto: String, accion: () -> Unit) {
    TextButton(onClick = accion, modifier = Modifier.padding(top = 4.dp)) {
        Text("$texto  →", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IndicadorInicio(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
        Text(texto, color = TextoSecundarioInicio, fontSize = 14.sp)
    }
}

@Composable
private fun MensajeErrorInicio(texto: String, reintentar: () -> Unit) {
    Column {
        Text(texto, color = Color(0xFFFF8D8D), fontSize = 15.sp, lineHeight = 20.sp)
        TextButton(onClick = reintentar) { Text("REINTENTAR", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}

private fun minutosEntre(inicio: String?, fin: String?): Int? {
    fun minutos(hora: String?): Int? {
        val partes = hora?.take(5)?.split(":") ?: return null
        return partes.getOrNull(0)?.toIntOrNull()?.let { h -> partes.getOrNull(1)?.toIntOrNull()?.let { m -> h * 60 + m } }
    }
    val a = minutos(inicio) ?: return null
    val b = minutos(fin) ?: return null
    return (b - a).takeIf { it > 0 }
}

private fun formatear(valor: BigDecimal?): String {
    if (valor == null) return "0"
    return DecimalFormat("#,##0.#").format(valor)
}
