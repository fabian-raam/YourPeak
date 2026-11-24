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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.EjercicioResponse
import com.ramirez.yourpeak.data.repository.EjercicioRepository
import com.ramirez.yourpeak.data.repository.RutinaRepository
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.components.BotonIconoEliminar
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

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
        Spacer(Modifier.height(8.dp))

        when {
            estado.cargando -> CircularProgressIndicator()
            estado.error != null && estado.diasGuardados.none { it.disponible } && estado.dias.none { it.disponible } -> {
                MensajeRutina(estado.error.orEmpty(), error = true)
                TextButton(onClick = viewModel::reintentar) { Text("Reintentar") }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    estado.dias.forEach { item ->
                        val seleccionado = estado.diaSeleccionado == item.numero
                        Surface(
                            modifier = Modifier.weight(1f).height(76.dp).clickable {
                                viewModel.seleccionarDia(item.numero)
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (seleccionado) MaterialTheme.colorScheme.primary else Color.Black,
                            border = BorderStroke(
                                1.dp,
                                if (seleccionado) MaterialTheme.colorScheme.primary else Color(0xFF303030)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    item.nombre.take(3).uppercase(),
                                    fontSize = 18.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                if (seleccionado) {
                                    Spacer(
                                        Modifier
                                            .padding(top = 3.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFB7F34A))
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                if (dia != null) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                dia.nombre.uppercase(),
                                fontSize = 17.sp,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.width(8.dp))
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF292929))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (dia.disponible) "Disponible · ${dia.horaInicio ?: "--:--"} a ${dia.horaFin ?: "--:--"}"
                            else "Día de descanso · configura tu disponibilidad para agregar ejercicios.",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFF292929))
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Ejercicios del día",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            val cantidadEjercicios = dia.ejercicios.size
                            Text(
                                cantidadEjercicios.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFF292929))
                        Spacer(Modifier.height(10.dp))

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
                                        quitar = { viewModel.quitarEjercicio(ejercicio.idEjercicio) }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { mostrarSelector = true },
                                enabled = estado.catalogo.isNotEmpty(),
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
                                    cancelar = { viewModel.seleccionarEjercicio(null) }
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

            }
        }
        }

        if (!estado.cargando && dia != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = viewModel::restablecer,
                        enabled = estado.hayCambios && !estado.guardando,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("RESTABLECER", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 0.4.sp)
                    }
                    Button(
                        onClick = viewModel::guardar,
                        enabled = estado.hayCambios && !estado.guardando,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(18.dp)
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
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
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
}

@Composable
private fun EjercicioAsignadoFila(ejercicio: EjercicioRutinaUi, quitar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF141414),
        border = BorderStroke(1.dp, Color(0xFF383838))
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        ejercicio.nombre,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colorGrupoMuscular(ejercicio.grupoMuscular))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            ejercicio.grupoMuscular,
                            fontSize = 16.sp,
                            color = Color(0xFFBFC3CC)
                        )
                    }
                }
                BotonIconoEliminar(onClick = quitar)
            }
            val medicion = if (ejercicio.tipoMedicion.equals("TIEMPO", ignoreCase = true)) {
                "${ejercicio.segundos ?: 0} s"
            } else "${ejercicio.repeticiones ?: 0} rep."
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetaDatoRutina("${ejercicio.series} series")
                EtiquetaDatoRutina(medicion)
                EtiquetaDatoRutina("Descanso ${ejercicio.descansoSegundos} s")
            }
        }
    }
}

@Composable
private fun EtiquetaDatoRutina(texto: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141414),
        border = BorderStroke(1.dp, Color(0xFF383838))
    ) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 14.sp,
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
    cancelar: () -> Unit
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
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Text("CANCELAR", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Button(
                    onClick = agregar,
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

private fun colorGrupoMuscular(grupoMuscular: String?): Color = when (grupoMuscular.orEmpty().lowercase()) {
    "pecho" -> Color(0xFF2677FF)
    "brazos" -> Color(0xFFFF5A36)
    "piernas" -> Color(0xFFB7F34A)
    "espalda" -> Color.White
    else -> Color(0xFFB7F34A)
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
                EjercicioAsignadoFila(
                    EjercicioRutinaUi(1, "Press de banca plano", "Pecho", "REPETICIONES", 4, 8, null, 90),
                    quitar = {}
                )
            }
        }
    }
}
