package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.EjercicioResponse
import com.ramirez.yourpeak.data.repository.EjercicioRepository
import com.ramirez.yourpeak.ui.components.colorGrupoMuscular
import com.ramirez.yourpeak.ui.components.IconoBuscar
import com.ramirez.yourpeak.ui.components.BotonIconoDetalle

@Composable
fun EjerciciosScreen(idUsuario: Long?, repository: EjercicioRepository) {
    val viewModel: EjerciciosViewModel = viewModel(
        key = "catalogo-ejercicios-$idUsuario",
        factory = EjerciciosViewModel.factory(repository, idUsuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var ejercicioDetalle by remember { mutableStateOf<EjercicioResponse?>(null) }

    ejercicioDetalle?.let { ejercicio ->
        DetalleEjercicioDialog(ejercicio = ejercicio, cerrar = { ejercicioDetalle = null })
    }

    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = estado.busqueda,
            onValueChange = viewModel::cambiarBusqueda,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            singleLine = true,
            placeholder = {
                Text("Buscar por ejercicio, grupo o equipo", fontSize = 16.sp, color = Color(0xFFB8BCC6))
            },
            leadingIcon = { IconoBuscar() },
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
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        if (estado.grupos.isNotEmpty()) {
            EncabezadoFiltro("Grupo muscular")
            FiltrosHorizontales(
                opciones = estado.grupos,
                seleccionado = estado.grupoSeleccionado,
                alSeleccionar = viewModel::cambiarGrupo
            )
        }

        if (estado.niveles.isNotEmpty()) {
            EncabezadoFiltro("Nivel")
            FiltrosHorizontales(
                opciones = estado.niveles,
                seleccionado = estado.nivelSeleccionado,
                alSeleccionar = viewModel::cambiarNivel
            )
        }

        Spacer(Modifier.height(14.dp))
        when {
            estado.cargando -> CircularProgressIndicator()
            estado.error != null -> {
                Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
                TextButton(onClick = viewModel::reintentar) { Text("Reintentar", fontSize = 16.sp) }
            }
            estado.ejerciciosFiltrados.isEmpty() -> Text(
                if (estado.ejercicios.isEmpty()) "Aún no hay ejercicios activos en el catálogo."
                else "No hay ejercicios que coincidan con la búsqueda y los filtros.",
                color = Color(0xFFB8BCC6),
                fontSize = 16.sp
            )
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${estado.ejerciciosFiltrados.size} EJERCICIOS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF292929)
                    )
                }
                Spacer(Modifier.height(8.dp))
                estado.ejerciciosFiltrados.forEachIndexed { index, ejercicio ->
                    EjercicioTarjeta(
                        ejercicio = ejercicio,
                        alVerDetalle = { ejercicioDetalle = ejercicio }
                    )
                    if (index < estado.ejerciciosFiltrados.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(color = Color(0xFF292929))
                    }
                }
            }
        }
    }
}

@Composable
private fun EncabezadoFiltro(texto: String) {
    Text(
        texto,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFFE0E2E6)
    )
}

@Composable
private fun FiltrosHorizontales(
    opciones: List<String>,
    seleccionado: String?,
    alSeleccionar: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FiltroEjercicio("Todos", seleccionado == null) { alSeleccionar(null) }
        opciones.forEach { opcion ->
            FiltroEjercicio(opcion, seleccionado == opcion) {
                alSeleccionar(if (seleccionado == opcion) null else opcion)
            }
        }
    }
}

@Composable
private fun FiltroEjercicio(texto: String, seleccionado: Boolean, alPulsar: () -> Unit) {
    Surface(
        modifier = Modifier.height(38.dp).clickable(onClick = alPulsar),
        shape = RoundedCornerShape(22.dp),
        color = if (seleccionado) MaterialTheme.colorScheme.primary else Color.Black,
        border = BorderStroke(1.dp, if (seleccionado) MaterialTheme.colorScheme.primary else Color(0xFF383838))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                texto.uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EjercicioTarjeta(ejercicio: EjercicioResponse, alVerDetalle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(
                ejercicio.nombre ?: "Ejercicio",
                fontSize = 18.sp,
                lineHeight = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(7.dp),
                    shape = CircleShape,
                    color = colorGrupoMuscular(ejercicio.grupoMuscular)
                ) {}
                Spacer(Modifier.width(6.dp))
                Text(
                    listOfNotNull(ejercicio.grupoMuscular, ejercicio.tipoMedicion)
                        .joinToString(" \u00b7 ").ifBlank { "Sin clasificaci\u00f3n" },
                    fontSize = 14.sp,
                    color = Color(0xFFBFC3CC)
                )
            }
        }
        BotonIconoDetalle(
            onClick = alVerDetalle,
            descripcion = "Ver detalle de ${ejercicio.nombre ?: "ejercicio"}"
        )
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun DetalleEjercicioDialog(ejercicio: EjercicioResponse, cerrar: () -> Unit) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recursoImagen = recursoImagenEjercicio(ejercicio)
    val medicion = when (ejercicio.tipoMedicion?.uppercase()) {
        "REPETICIONES" -> "Repeticiones"
        "TIEMPO" -> "Tiempo"
        else -> ejercicio.tipoMedicion
    }

    ModalBottomSheet(
        onDismissRequest = cerrar,
        sheetState = estadoHoja,
        containerColor = Color(0xFF101010),
        contentColor = Color.White,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(horizontal = 22.dp)
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            ejercicio.nombre ?: "Ejercicio",
                            fontSize = 20.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        ejercicio.descripcion?.takeIf(String::isNotBlank)?.let {
                            Spacer(Modifier.height(10.dp))
                            Text(it, fontSize = 16.sp, lineHeight = 22.sp, color = Color(0xFFBFC3CC))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    TextButton(onClick = cerrar) {
                        Text("\u00d7", fontSize = 25.sp, color = Color.White)
                    }
                }

                Spacer(Modifier.height(16.dp))
                EncabezadoDetalle("EL MOVIMIENTO")
                Spacer(Modifier.height(10.dp))
                if (recursoImagen != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        Column {
                            Image(
                                painter = painterResource(recursoImagen),
                                contentDescription = "Ilustraci\u00f3n del ejercicio ${ejercicio.nombre}",
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                contentScale = ContentScale.Fit
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .background(Color(0xFF151515))
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (ejercicio.imagenRuta.equals("plancha-frontal", ignoreCase = true)) {
                                    Text("Posici\u00f3n de sost\u00e9n", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                } else {
                                    Text("1  Inicio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("2  Final", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF171717),
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Text("Ilustraci\u00f3n no disponible", fontSize = 16.sp, color = Color(0xFFBFC3CC))
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                EncabezadoDetalle("DATOS")
                Spacer(Modifier.height(8.dp))
                DetalleEjercicioCampo("Grupo muscular", ejercicio.grupoMuscular)
                DetalleEjercicioCampo("Nivel", ejercicio.nivelExperiencia)
                DetalleEjercicioCampo("Equipamiento", ejercicio.equipamiento)
                DetalleEjercicioCampo("Tipo de medici\u00f3n", medicion)
                Spacer(Modifier.height(18.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF101010),
                border = BorderStroke(1.dp, Color(0xFF383838))
            ) {
                TextButton(onClick = cerrar, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("CERRAR", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EncabezadoDetalle(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(texto, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, color = Color.White)
        Spacer(Modifier.width(10.dp))
        androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF303030))
    }
}

@Composable
private fun DetalleEjercicioCampo(etiqueta: String, valor: String?) {
    valor?.takeIf(String::isNotBlank)?.let {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(etiqueta, fontSize = 15.sp, color = Color(0xFFBFC3CC))
                Spacer(Modifier.width(12.dp))
                Text(it, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            androidx.compose.material3.HorizontalDivider(color = Color(0xFF303030))
        }
    }
}

private fun recursoImagenEjercicio(ejercicio: EjercicioResponse): Int? {
    fun normalizar(valor: String): String = java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

    val claveRuta = ejercicio.imagenRuta
        ?.substringAfterLast('/')
        ?.substringBeforeLast('.')
        ?.takeIf(String::isNotBlank)
    val clave = normalizar(claveRuta ?: ejercicio.nombre.orEmpty()).replace('-', '_')
    return when (clave) {
        "aperturas_con_mancuernas" -> com.ramirez.yourpeak.R.drawable.exercise_aperturas_con_mancuernas
        "curl_de_biceps" -> com.ramirez.yourpeak.R.drawable.exercise_curl_de_biceps
        "elevaciones_laterales" -> com.ramirez.yourpeak.R.drawable.exercise_elevaciones_laterales
        "extension_de_triceps_en_polea" -> com.ramirez.yourpeak.R.drawable.exercise_extension_de_triceps_en_polea
        "flexiones_de_pecho" -> com.ramirez.yourpeak.R.drawable.exercise_flexiones_de_pecho
        "jalon_al_pecho" -> com.ramirez.yourpeak.R.drawable.exercise_jalon_al_pecho
        "peso_muerto_rumano" -> com.ramirez.yourpeak.R.drawable.exercise_peso_muerto_rumano
        "plancha_frontal" -> com.ramirez.yourpeak.R.drawable.exercise_plancha_frontal
        "prensa_de_piernas" -> com.ramirez.yourpeak.R.drawable.exercise_prensa_de_piernas
        "press_de_banca" -> com.ramirez.yourpeak.R.drawable.exercise_press_de_banca
        "press_inclinado_con_mancuernas" -> com.ramirez.yourpeak.R.drawable.exercise_press_inclinado_con_mancuernas
        "press_militar" -> com.ramirez.yourpeak.R.drawable.exercise_press_militar
        "remo_con_mancuerna" -> com.ramirez.yourpeak.R.drawable.exercise_remo_con_mancuerna
        "saltos_de_tijera" -> com.ramirez.yourpeak.R.drawable.exercise_saltos_de_tijera
        "sentadilla_goblet" -> com.ramirez.yourpeak.R.drawable.exercise_sentadilla_goblet
        "zancadas_caminando" -> com.ramirez.yourpeak.R.drawable.exercise_zancadas_caminando
        else -> null
    }
}
