package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.AlimentoNutricionResponse
import com.ramirez.yourpeak.data.repository.NutricionRepository
import com.ramirez.yourpeak.domain.Fechas
import com.ramirez.yourpeak.ui.components.BotonIconoEditar
import com.ramirez.yourpeak.ui.components.BotonIconoEliminar
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.components.IconoBuscar
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

    Column(Modifier.fillMaxWidth()) {
        if (estado.error != null || estado.errorCatalogo != null || estado.aviso != null) {
            MensajeNutricion(
                texto = estado.error ?: estado.errorCatalogo ?: estado.aviso.orEmpty(),
                esError = estado.error != null || estado.errorCatalogo != null,
                cerrar = viewModel::cerrarMensajes
            )
            Spacer(Modifier.height(12.dp))
        }

        ResumenNutricionCuadricula(
            kcal = estado.kcal,
            proteina = estado.proteinaG,
            carbohidratos = estado.carbohidratoG,
            grasas = estado.grasaG,
            cargando = estado.cargandoDia
        )
        Spacer(Modifier.height(18.dp))

        Text("REGISTRO DIARIO", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF141414),
            border = BorderStroke(1.dp, Color(0xFF303030))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotonCambioDia("‹", habilitado = true) { viewModel.moverDia(-1) }
                Text(
                    formatearFechaCorta(estado.fecha),
                    modifier = Modifier.weight(1f).clickable { mostrarCalendario = true },
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1
                )
                BotonCambioDia("›", habilitado = !estado.esHoy) { viewModel.moverDia(1) }
            }
        }

        if (estado.esHoy) {
            Spacer(Modifier.height(18.dp))
            Text("AGREGAR ALIMENTO", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 0.4.sp, color = Color.White)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = estado.busqueda,
                onValueChange = viewModel::buscar,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                placeholder = { Text("Buscar alimento", fontSize = 16.sp, color = Color(0xFFB8BCC6)) },
                leadingIcon = { IconoBuscar() },
                singleLine = true,
                enabled = !estado.cargandoCatalogo && !estado.guardando,
                shape = RoundedCornerShape(22.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF141414),
                    unfocusedContainerColor = Color(0xFF141414),
                    focusedIndicatorColor = Color(0xFF454545),
                    unfocusedIndicatorColor = Color(0xFF303030),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(52.dp)
                    .clickable(enabled = !estado.cargandoCatalogo && estado.alimentos.isNotEmpty() && !estado.guardando) {
                        mostrarSelectorAlimento = true
                    },
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF141414),
                border = BorderStroke(1.dp, Color(0xFF303030))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        alimentoSeleccionado?.nombre ?: if (estado.cargandoCatalogo) "Cargando alimentos..." else "Selecciona un alimento",
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        color = if (alimentoSeleccionado == null) Color(0xFFB8BCC6) else Color.White,
                        maxLines = 1
                    )
                    Text("⌄", fontSize = 20.sp, color = Color(0xFFB8BCC6))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = estado.cantidadG,
                onValueChange = viewModel::cambiarCantidad,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                placeholder = { Text("Cantidad (g)", fontSize = 16.sp, color = Color(0xFFB8BCC6)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = !estado.guardando,
                shape = RoundedCornerShape(18.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF141414),
                    unfocusedContainerColor = Color(0xFF141414),
                    focusedIndicatorColor = Color(0xFF454545),
                    unfocusedIndicatorColor = Color(0xFF303030),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            nutrientesPrevia?.let {
                Text(
                    "Aprox.: ${formatearNumero(it[0])} kcal · ${formatearNumero(it[1])} g proteína",
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 14.sp,
                    color = Color(0xFFB8BCC6)
                )
            }
            if (estado.alimentos.isEmpty() && !estado.cargandoCatalogo) {
                TextButton(onClick = viewModel::reintentarCatalogo) { Text("Volver a cargar alimentos", fontSize = 15.sp) }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::registrar,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = alimentoSeleccionado != null && !estado.guardando && !estado.cargandoCatalogo,
                shape = RoundedCornerShape(26.dp)
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (estado.guardando) "AGREGANDO..." else "AGREGAR", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                "Día anterior: puedes revisar, corregir o eliminar consumos registrados.",
                fontSize = 15.sp,
                color = Color(0xFFB8BCC6)
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ALIMENTOS REGISTRADOS", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("${estado.ingestas.size}", fontSize = 15.sp, color = Color(0xFFB8BCC6))
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.HorizontalDivider(color = Color(0xFF303030))
        when {
            estado.cargandoDia -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(18.dp))
            estado.ingestas.isEmpty() -> Text(
                if (estado.esHoy) "Aún no registraste alimentos hoy." else "No hay consumos en esta fecha.",
                modifier = Modifier.padding(vertical = 18.dp),
                fontSize = 15.sp,
                color = Color(0xFFB8BCC6)
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

    if (mostrarSelectorAlimento) {
        SelectorAlimentoBottomSheet(
            alimentos = estado.alimentos,
            busquedaInicial = estado.busqueda,
            cambiarBusqueda = viewModel::buscar,
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
private fun ResumenNutricionCuadricula(
    kcal: BigDecimal,
    proteina: BigDecimal,
    carbohidratos: BigDecimal,
    grasas: BigDecimal,
    cargando: Boolean
) {
    val colorDivision = Color(0xFF454545)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Black,
        border = BorderStroke(1.dp, Color(0xFF383838))
    ) {
        Column {
            Row(Modifier.height(IntrinsicSize.Min)) {
                ResumenNutricion("Calorías", kcal, "kcal", cargando, Color(0xFF2878FF), Modifier.weight(1f))
                Spacer(Modifier.fillMaxHeight().width(1.dp).background(colorDivision))
                ResumenNutricion("Proteína", proteina, "g", cargando, Color(0xFFB7F34A), Modifier.weight(1f))
            }
            Row(Modifier.height(IntrinsicSize.Min)) {
                ResumenNutricion("Carbohidratos", carbohidratos, "g", cargando, Color(0xFFFF6B35), Modifier.weight(1f))
                Spacer(Modifier.fillMaxHeight().width(1.dp).background(colorDivision))
                ResumenNutricion("Grasas", grasas, "g", cargando, Color.White, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResumenNutricion(
    titulo: String,
    valor: BigDecimal,
    unidad: String,
    cargando: Boolean,
    acento: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Spacer(Modifier.fillMaxWidth().height(2.dp).background(acento))
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (cargando) "—" else formatearNumero(valor),
                    fontSize = 34.sp,
                    lineHeight = 37.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(Modifier.width(3.dp))
                Text(unidad, modifier = Modifier.padding(bottom = 4.dp), fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB8BCC6))
            }
            Text(titulo, fontSize = 16.sp, lineHeight = 19.sp, color = Color(0xFFB8BCC6), maxLines = 1)
        }
    }
}

@Composable
private fun BotonCambioDia(texto: String, habilitado: Boolean, alPulsar: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clickable(enabled = habilitado, onClick = alPulsar),
        shape = RoundedCornerShape(14.dp),
        color = if (habilitado) Color(0xFF202020) else Color(0xFF111111)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(texto, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = if (habilitado) Color.White else Color(0xFF666666))
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
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF141414),
        border = BorderStroke(1.dp, Color(0xFF383838))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(ingesta.alimento, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${formatearHora(ingesta.fechaHora)} · ${formatearNumero(ingesta.cantidadG)} g",
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFB8BCC6)
                    )
                }
                if (!editando) {
                    Spacer(Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BotonIconoEditar(
                            onClick = iniciarEdicion,
                            descripcion = "Editar cantidad de ${ingesta.alimento}",
                            habilitado = !guardando
                        )
                        BotonIconoEliminar(
                            onClick = eliminar,
                            descripcion = "Eliminar ${ingesta.alimento}",
                            habilitado = !guardando
                        )
                    }
                }
            }
            if (editando) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = cantidadEditada,
                    onValueChange = cambiarCantidad,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cantidad (g)", fontSize = 16.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = !guardando,
                    shape = RoundedCornerShape(18.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = Color.White),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF141414),
                        unfocusedContainerColor = Color(0xFF141414),
                        focusedIndicatorColor = Color(0xFF454545),
                        unfocusedIndicatorColor = Color(0xFF383838),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(modifier = Modifier.align(Alignment.End)) {
                    TextButton(onClick = cancelarEdicion, enabled = !guardando) { Text("Cancelar", fontSize = 15.sp) }
                    TextButton(onClick = guardarEdicion, enabled = !guardando) { Text("Guardar", fontSize = 15.sp) }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EtiquetaNutricion("${formatearNumero(ingesta.cantidadG)} g")
                    EtiquetaNutricion("${formatearNumero(ingesta.kcal)} kcal")
                    EtiquetaNutricion("${formatearNumero(ingesta.proteinaG)} g prot.")
                    EtiquetaNutricion("${formatearNumero(ingesta.carbohidratoG)} g carb.")
                    EtiquetaNutricion("${formatearNumero(ingesta.grasaG)} g grasa")
                }
            }
        }
    }
}

@Composable
private fun EtiquetaNutricion(texto: String) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF141414),
        border = BorderStroke(1.dp, Color(0xFF383838))
    ) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SelectorAlimentoBottomSheet(
    alimentos: List<AlimentoNutricionResponse>,
    busquedaInicial: String,
    cambiarBusqueda: (String) -> Unit,
    seleccionar: (Int) -> Unit,
    cerrar: () -> Unit
) {
    var busqueda by remember { mutableStateOf(busquedaInicial) }
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filtrados = alimentos.filter { alimento ->
        busqueda.isBlank() || alimento.nombre.orEmpty().contains(busqueda.trim(), ignoreCase = true)
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
            Text("Elige un alimento", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = busqueda,
                onValueChange = {
                    busqueda = it
                    cambiarBusqueda(it)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar", color = Color(0xFF9A9A9A), fontSize = 16.sp) },
                leadingIcon = { IconoBuscar() },
                shape = RoundedCornerShape(20.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = Color.White),
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
            androidx.compose.material3.HorizontalDivider(color = Color(0xFF303030))
            if (filtrados.isEmpty()) {
                Text(
                    if (alimentos.isEmpty()) "No hay alimentos disponibles." else "No hay alimentos que coincidan con la búsqueda.",
                    modifier = Modifier.padding(vertical = 20.dp),
                    color = Color(0xFFB8BCC6),
                    fontSize = 14.sp
                )
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    filtrados.forEach { alimento ->
                        val id = alimento.idAlimento
                        if (id != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth().clickable { seleccionar(id) }.padding(vertical = 10.dp)
                            ) {
                                Text(alimento.nombre.orEmpty(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Porción base ${formatearNumero(alimento.porcionBaseG ?: BigDecimal.ZERO)} g · ${formatearNumero(alimento.kcal ?: BigDecimal.ZERO)} kcal",
                                    fontSize = 13.sp,
                                    color = Color(0xFFB8BCC6)
                                )
                            }
                            androidx.compose.material3.HorizontalDivider(color = Color(0xFF303030))
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
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("CERRAR", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
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
        shape = RoundedCornerShape(18.dp),
        color = if (esError) Color(0xFF241516) else Color(0xFF11130B),
        border = BorderStroke(1.dp, if (esError) Color(0xFF713B3E) else Color(0xFF46551A))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!esError) {
                Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = Color(0xFF29360A)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✓", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFB7F34A))
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(
                texto,
                modifier = Modifier.weight(1f),
                color = if (esError) Color(0xFFFFB4B4) else Color.White,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = cerrar) {
                Text(if (esError) "×" else "Cerrar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
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

private fun formatearFechaCorta(iso: String): String {
    val fecha = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(iso)
    }.getOrNull() ?: return iso
    val localEspanol = Locale.forLanguageTag("es-PE")
    val texto = SimpleDateFormat("EEE, d MMM yyyy", localEspanol).apply {
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
                ResumenNutricionCuadricula(
                    kcal = BigDecimal("1250"),
                    proteina = BigDecimal("80"),
                    carbohidratos = BigDecimal("150"),
                    grasas = BigDecimal("45"),
                    cargando = false
                )
            }
        }
    }
}
