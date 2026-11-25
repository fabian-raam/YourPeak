package com.ramirez.yourpeak.ui.home

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import com.ramirez.yourpeak.ui.components.IlustracionEjercicio
import com.ramirez.yourpeak.ui.theme.BarlowCondensed
import com.ramirez.yourpeak.ui.theme.Carbon
import com.ramirez.yourpeak.ui.theme.CarbonBorde
import com.ramirez.yourpeak.ui.theme.Lima
import java.util.Calendar
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.EjercicioResponse
import com.ramirez.yourpeak.data.repository.EjercicioRepository
import com.ramirez.yourpeak.data.repository.RutinaRepository
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.components.BotonIconoEliminar
import com.ramirez.yourpeak.ui.components.BotonIconoRealizado
import com.ramirez.yourpeak.ui.components.colorGrupoMuscular
import com.ramirez.yourpeak.ui.theme.YourPeakTheme

@Composable
fun MiRutinaScreen(
    modifier: Modifier = Modifier,
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
    var ejercicioAConfirmar by remember { mutableStateOf<EjercicioRutinaUi?>(null) }
    val diaHoy = ((Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Lima")).get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
        Spacer(Modifier.height(8.dp))

        when {
            estado.cargando -> Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Cargando tu rutina…", fontSize = 15.sp, color = Color(0xFF9AA1AD))
            }
            estado.error != null && estado.dias.none { it.disponible } -> {
                MensajeRutina(estado.error.orEmpty(), error = true)
                TextButton(onClick = viewModel::reintentar) { Text("Reintentar") }
            }
            else -> {
                TarjetaRutinaIa(
                    propuesta = estado.propuesta,
                    generando = estado.generando,
                    habilitado = estado.puedeGenerar,
                    hayDiasDisponibles = estado.dias.any { it.disponible },
                    generar = viewModel::generar
                )
                Spacer(Modifier.height(16.dp))
                SelectorDiasSemana(
                    dias = estado.dias,
                    seleccionado = estado.diaSeleccionado,
                    alSeleccionar = viewModel::seleccionarDia
                )

                Spacer(Modifier.height(18.dp))
                if (dia != null) {
                    Column(Modifier.fillMaxWidth()) {
                        EncabezadoDiaRutina(dia)
                        Spacer(Modifier.height(12.dp))

                        if (!dia.disponible) {
                            TextButton(onClick = alIrADisponibilidad) { Text("Configurar disponibilidad") }
                        } else {
                            if (dia.ejercicios.isEmpty()) {
                                Text(
                                    "Todavía no agregaste ejercicios para este día.",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                dia.ejercicios.forEach { ejercicio ->
                                    EjercicioAsignadoFila(
                                        ejercicio = ejercicio,
                                        quitar = { viewModel.quitarEjercicio(ejercicio.idEjercicio) },
                                        marcarRealizado = { ejercicioAConfirmar = ejercicio },
                                        realizado = ejercicio.idPlanEjercicio?.let { it in estado.ejerciciosCompletados } == true,
                                        puedeMarcarRealizado = !estado.registrandoRealizado,
                                        habilitado = !estado.guardando && !estado.generando && !estado.registrandoRealizado
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { mostrarSelector = true },
                                enabled = estado.catalogo.isNotEmpty() && !estado.generando && !estado.guardando,
                                modifier = Modifier.height(50.dp),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text("AGREGAR EJERCICIO", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                            }

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
                                    cancelar = { viewModel.seleccionarEjercicio(null) },
                                    habilitado = !estado.guardando
                                )
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
                if (estado.guardando) {
                    Spacer(Modifier.height(8.dp))
                    Text("Guardando rutina…", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "La rutina se repite cada semana.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

            }
        }
        }
    }

    if (mostrarSelector && dia != null) {
        SelectorEjercicioBottomSheet(
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

    ejercicioAConfirmar?.let { ejercicio ->
        ConfirmarEjercicioRealizadoDialog(
            ejercicio = ejercicio,
            puedeRegistrarHoy = estado.diaSeleccionado == diaHoy,
            confirmar = {
                viewModel.marcarEjercicioRealizado(ejercicio)
                ejercicioAConfirmar = null
            },
            cancelar = { ejercicioAConfirmar = null }
        )
    }
}

@Composable
private fun EjercicioAsignadoFila(
    ejercicio: EjercicioRutinaUi,
    quitar: () -> Unit,
    marcarRealizado: () -> Unit,
    realizado: Boolean,
    puedeMarcarRealizado: Boolean,
    habilitado: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Carbon,
        border = BorderStroke(1.dp, CarbonBorde)
    ) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            IlustracionEjercicio(
                imagenRuta = ejercicio.imagenRuta,
                grupoMuscular = ejercicio.grupoMuscular,
                forma = RoundedCornerShape(12.dp),
                modifier = Modifier.width(76.dp).height(60.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    ejercicio.nombre,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)) {
                    Spacer(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colorGrupoMuscular(ejercicio.grupoMuscular))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(ejercicio.grupoMuscular, fontSize = 13.sp, color = Color(0xFF9AA1AD))
                    if (ejercicio.origen == ORIGEN_IA) {
                        Spacer(Modifier.width(8.dp))
                        EtiquetaIa()
                    }
                }
                val medicion = if (ejercicio.tipoMedicion.equals("TIEMPO", ignoreCase = true)) {
                    "${ejercicio.series} × ${ejercicio.segundos ?: 0} s"
                } else "${ejercicio.series} × ${ejercicio.repeticiones ?: 0}"
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EtiquetaDatoRutina(medicion)
                    EtiquetaDatoRutina(if (ejercicio.descansoSegundos == 0) "Sin descanso" else "${ejercicio.descansoSegundos} s descanso")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                BotonIconoRealizado(
                    onClick = marcarRealizado,
                    realizado = realizado,
                    habilitado = habilitado && puedeMarcarRealizado
                )
                BotonIconoEliminar(onClick = quitar, habilitado = habilitado)
            }
        }
    }
}

@Composable
private fun ConfirmarEjercicioRealizadoDialog(
    ejercicio: EjercicioRutinaUi,
    puedeRegistrarHoy: Boolean,
    confirmar: () -> Unit,
    cancelar: () -> Unit
) {
    Dialog(onDismissRequest = cancelar) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF151515),
            border = BorderStroke(1.dp, Color(0xFF383838))
        ) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text(
                    "¿Completaste este ejercicio?",
                    fontFamily = BarlowCondensed,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    ejercicio.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                val detalle = if (ejercicio.tipoMedicion.equals("TIEMPO", ignoreCase = true)) {
                    "${ejercicio.series} series de ${ejercicio.segundos ?: 0} s"
                } else {
                    "${ejercicio.series} series de ${ejercicio.repeticiones ?: 0} repeticiones"
                }
                Text(
                    if (puedeRegistrarHoy) "$detalle. Se registrará como realizado hoy."
                    else "$detalle. Para registrarlo, selecciona el día de hoy en tu rutina.",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = if (puedeRegistrarHoy) Color(0xFFB8BCC6) else Color(0xFFEBD39B)
                )
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = cancelar,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF454545))
                    ) {
                        Text("CANCELAR", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Button(
                        onClick = confirmar,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Lima,
                            contentColor = Color(0xFF101010),
                            disabledContainerColor = Color(0xFF303326),
                            disabledContentColor = Color(0xFF777777)
                        ),
                        enabled = puedeRegistrarHoy
                    ) {
                        Text("CONFIRMAR", fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/** Lunes a domingo de la semana actual: número del día y un punto si tiene ejercicios. */
@Composable
private fun SelectorDiasSemana(dias: List<DiaRutinaUi>, seleccionado: Int, alSeleccionar: (Int) -> Unit) {
    val numeros = remember {
        val calendario = Calendar.getInstance()
        val diaHoy = ((calendario.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
        calendario.add(Calendar.DAY_OF_MONTH, 1 - diaHoy)
        (1..7).map { calendario.get(Calendar.DAY_OF_MONTH).also { calendario.add(Calendar.DAY_OF_MONTH, 1) } }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        dias.forEachIndexed { indice, dia ->
            val elegido = dia.numero == seleccionado
            val primario = MaterialTheme.colorScheme.primary
            Surface(
                modifier = Modifier.weight(1f).clickable { alSeleccionar(dia.numero) },
                shape = RoundedCornerShape(14.dp),
                color = if (elegido) primario else Carbon,
                border = BorderStroke(1.dp, if (elegido) primario else CarbonBorde)
            ) {
                Column(
                    Modifier.padding(vertical = 7.dp).fillMaxWidth().alpha(if (dia.disponible || elegido) 1f else 0.5f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        dia.nombre.take(3).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (elegido) Color.White else Color(0xFF9AA1AD)
                    )
                    Text(
                        numeros.getOrElse(indice) { dia.numero }.toString(),
                        fontFamily = BarlowCondensed,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        lineHeight = 23.sp,
                        color = Color.White
                    )
                    Box(
                        Modifier
                            .padding(top = 2.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    dia.ejercicios.isEmpty() -> Color.Transparent
                                    elegido -> Color.White
                                    else -> Lima
                                }
                            )
                    )
                }
            }
        }
    }
}

/** Encabezado del día: horario en pequeño, grupo muscular principal en grande y cantidad de ejercicios. */
@Composable
private fun EncabezadoDiaRutina(dia: DiaRutinaUi) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(
                if (dia.disponible) "${dia.nombre.uppercase()} · ${dia.horaInicio ?: "--:--"} – ${dia.horaFin ?: "--:--"}"
                else dia.nombre.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFF9AA1AD)
            )
            Text(
                when {
                    !dia.disponible -> "DESCANSO"
                    dia.ejercicios.isEmpty() -> "DÍA DISPONIBLE"
                    else -> dia.ejercicios.map { it.grupoMuscular }.filter(String::isNotBlank)
                        .distinct().take(2).joinToString(" + ").uppercase().ifBlank { "ENTRENAMIENTO" }
                },
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                lineHeight = 34.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (dia.disponible) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1A1C21), border = BorderStroke(1.dp, CarbonBorde)) {
                Text(
                    "${dia.ejercicios.size} ${if (dia.ejercicios.size == 1) "ejercicio" else "ejercicios"}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF9AA1AD)
                )
            }
        }
    }
    if (!dia.disponible) {
        Text(
            "No marcaste este día como disponible para entrenar.",
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 14.sp,
            color = Color(0xFF9AA1AD)
        )
    }
}

/** Tarjeta para pedir una rutina al generador con IA; la propuesta no se guarda hasta pulsar «Guardar». */
@Composable
private fun TarjetaRutinaIa(
    propuesta: PropuestaIa?,
    generando: Boolean,
    habilitado: Boolean,
    hayDiasDisponibles: Boolean,
    generar: () -> Unit
) {
    val primario = MaterialTheme.colorScheme.primary
    if (propuesta != null) {
        // Con una propuesta en pantalla la tarjeta se reduce para dejar sitio a la rutina.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = primario.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, primario)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", fontSize = 18.sp, color = primario)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (propuesta.conGemini) "Propuesta generada con IA" else "Propuesta generada por reglas",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("La rutina ya quedó guardada.", fontSize = 12.sp, color = Color(0xFF9AA1AD))
                    }
                    OutlinedButton(
                        onClick = generar,
                        enabled = habilitado,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, primario),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (generando) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = primario)
                        } else Text("Otra", color = primario, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    "Rutina orientativa. Si tienes lesiones o dudas de salud, consulta con un entrenador.",
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFF9AA1AD)
                )
            }
        }
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Carbon,
        border = BorderStroke(1.dp, CarbonBorde)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = primario.copy(alpha = 0.16f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✦", fontSize = 17.sp, color = primario)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("Rutina con IA", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (hayDiasDisponibles) "Generaremos y guardaremos una semana de entrenamiento según tu perfil, tus horarios y el catálogo del gimnasio."
                else "Marca al menos un día en tu disponibilidad para generar una rutina.",
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = Color(0xFFBFC3CC)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Rutina orientativa. Si tienes lesiones o dudas de salud, consulta con un entrenador.",
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color(0xFF9AA1AD)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = generar,
                enabled = habilitado,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (generando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (generando) "GENERANDO" else "✦  GENERAR CON IA",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun EtiquetaIa() {
    val primario = MaterialTheme.colorScheme.primary
    Surface(shape = RoundedCornerShape(6.dp), color = primario.copy(alpha = 0.18f)) {
        Text(
            "IA",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
            color = primario
        )
    }
}

@Composable
private fun EtiquetaDatoRutina(texto: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1A1C21),
        border = BorderStroke(1.dp, CarbonBorde)
    ) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE0E2E6),
            maxLines = 1
        )
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
    cancelar: () -> Unit,
    habilitado: Boolean
) {
    val esTiempo = ejercicio.tipoMedicion.equals("TIEMPO", ignoreCase = true)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF141414),
        border = BorderStroke(1.dp, Color(0xFF292929))
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                ejercicio.nombre.orEmpty(),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoNumero(
                    valor = series,
                    etiqueta = "Series",
                    cambiar = cambiarSeries,
                    modifier = Modifier.weight(1f)
                )
                CampoNumero(
                    valor = valorMedicion,
                    etiqueta = if (esTiempo) "Tiempo (s)" else "Repeticiones",
                    cambiar = cambiarValorMedicion,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))
            CampoNumero(
                valor = descanso,
                etiqueta = "Descanso (s)",
                cambiar = cambiarDescanso,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = cancelar,
                    enabled = habilitado,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Text("CANCELAR", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Button(
                    onClick = agregar,
                    enabled = habilitado,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text("AGREGAR AL D\u00CDA", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
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
    Column(modifier) {
        Text(etiqueta, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE0E2E6))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = valor,
            onValueChange = cambiar,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF151515),
                unfocusedContainerColor = Color(0xFF151515),
                focusedIndicatorColor = Color(0xFF343434),
                unfocusedIndicatorColor = Color(0xFF292929),
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SelectorEjercicioBottomSheet(
    ejercicios: List<EjercicioResponse>,
    seleccionar: (Int?) -> Unit,
    cerrar: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filtrados = ejercicios.filter { ejercicio ->
        listOfNotNull(ejercicio.nombre, ejercicio.grupoMuscular)
            .any { it.contains(busqueda, ignoreCase = true) }
    }
    ModalBottomSheet(
        onDismissRequest = cerrar,
        sheetState = estadoHoja,
        containerColor = Color(0xFF121212),
        contentColor = Color.White,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp).width(38.dp).height(4.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xFF555555)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 22.dp)
        ) {
            Text("Elige un ejercicio", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar", color = Color(0xFF9A9A9A)) },
                leadingIcon = {
                    Canvas(Modifier.size(18.dp)) {
                        val color = Color(0xFFAAAAAA)
                        val lineWidth = 1.5.dp.toPx()
                        val center = Offset(size.width * 0.42f, size.height * 0.42f)
                        drawCircle(color, radius = size.minDimension * 0.28f, center = center, style = Stroke(lineWidth))
                        drawLine(
                            color,
                            start = Offset(center.x + size.width * 0.2f, center.y + size.height * 0.2f),
                            end = Offset(size.width * 0.9f, size.height * 0.9f),
                            strokeWidth = lineWidth
                        )
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF121212),
                    unfocusedContainerColor = Color(0xFF121212),
                    focusedIndicatorColor = Color(0xFF454545),
                    unfocusedIndicatorColor = Color(0xFF383838),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF303030))
            if (filtrados.isEmpty()) {
                Text(
                    "No hay ejercicios disponibles que coincidan.",
                    modifier = Modifier.padding(vertical = 20.dp),
                    color = Color(0xFFB8BCC6),
                    fontSize = 14.sp
                )
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    filtrados.forEach { ejercicio ->
                        val id = ejercicio.idEjercicio
                        if (id != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth().clickable { seleccionar(id) }.padding(vertical = 10.dp)
                            ) {
                                Text(ejercicio.nombre.orEmpty(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier.size(6.dp),
                                        shape = CircleShape,
                                        color = colorGrupoMuscular(ejercicio.grupoMuscular)
                                    ) {}
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${ejercicio.grupoMuscular.orEmpty()} · ${ejercicio.tipoMedicion.orEmpty().replaceFirstChar { it.uppercase() }}",
                                        fontSize = 13.sp,
                                        color = Color(0xFFB8BCC6)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFF303030))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = cerrar,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color(0xFF383838)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("CERRAR", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MensajeRutina(mensaje: String, error: Boolean, cerrar: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (error) Color(0xFF241516) else Color(0xFF11130B),
        border = BorderStroke(
            1.dp,
            if (error) Color(0xFF713B3E) else Color(0xFF46551A)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!error) {
                Surface(
                modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = Color(0xFF29360A)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✓", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFB7F34A))
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(
                mensaje,
                modifier = Modifier.weight(1f),
                color = if (error) Color(0xFFFFB4B4) else Color.White,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            cerrar?.let {
                TextButton(
                    onClick = it,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Cerrar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
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
                TarjetaRutinaIa(
                    propuesta = PropuestaIa("GEMINI", "Rutina propuesta con IA a partir de tu perfil y tus horarios."),
                    generando = false,
                    habilitado = true,
                    hayDiasDisponibles = true,
                    generar = {}
                )
                Spacer(Modifier.height(12.dp))
                EjercicioAsignadoFila(
                    EjercicioRutinaUi(1, "Press de banca plano", "Pecho", "REPETICIONES", 4, 8, null, 90, ORIGEN_IA),
                    quitar = {},
                    marcarRealizado = {},
                    realizado = false,
                    puedeMarcarRealizado = true,
                    habilitado = true
                )
            }
        }
    }
}
