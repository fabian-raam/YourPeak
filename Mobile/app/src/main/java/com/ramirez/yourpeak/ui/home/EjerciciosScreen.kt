package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.EjercicioResponse
import com.ramirez.yourpeak.data.repository.EjercicioRepository
import com.ramirez.yourpeak.ui.components.colorGrupoMuscular

@Composable
fun EjerciciosScreen(idUsuario: Long?, repository: EjercicioRepository) {
    val viewModel: EjerciciosViewModel = viewModel(
        key = "catalogo-ejercicios-$idUsuario",
        factory = EjerciciosViewModel.factory(repository, idUsuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()

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
                        expandido = ejercicio.idEjercicio == estado.ejercicioSeleccionado,
                        alPulsar = {
                            viewModel.seleccionarEjercicio(
                                if (ejercicio.idEjercicio == estado.ejercicioSeleccionado) null
                                else ejercicio.idEjercicio
                            )
                        }
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
private fun EjercicioTarjeta(ejercicio: EjercicioResponse, expandido: Boolean, alPulsar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = alPulsar).padding(vertical = 12.dp)
    ) {
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
                listOfNotNull(ejercicio.grupoMuscular, ejercicio.nivelExperiencia)
                    .joinToString(" · ").ifBlank { "Sin clasificación" },
                fontSize = 14.sp,
                color = Color(0xFFBFC3CC)
            )
        }
        if (expandido) {
            ejercicio.descripcion?.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 16.sp, lineHeight = 22.sp, color = Color(0xFFE0E2E6))
            }
            ejercicio.equipamiento?.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.height(4.dp))
                Text("Equipo: $it", fontSize = 15.sp, color = Color(0xFFBFC3CC))
            }
            ejercicio.tipoMedicion?.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.height(4.dp))
                Text("Medición: $it", fontSize = 15.sp, color = Color(0xFFBFC3CC))
            }
        }
    }
}

@Composable
private fun IconoBuscar() {
    Canvas(Modifier.size(22.dp)) {
        val centro = Offset(size.width * 0.43f, size.height * 0.43f)
        drawCircle(Color(0xFFB8BCC6), radius = size.minDimension * 0.25f, center = centro, style = Stroke(width = 1.8.dp.toPx()))
        drawLine(
            Color(0xFFB8BCC6),
            Offset(size.width * 0.62f, size.height * 0.62f),
            Offset(size.width * 0.83f, size.height * 0.83f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
