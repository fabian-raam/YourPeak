package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow
import com.ramirez.yourpeak.ui.components.IlustracionEjercicio
import com.ramirez.yourpeak.ui.theme.BarlowCondensed
import com.ramirez.yourpeak.ui.theme.Carbon
import com.ramirez.yourpeak.ui.theme.CarbonBorde
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
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
                Text("Buscar por ejercicio, grupo o equipo", fontSize = 16.sp, color = Color(0xFF9AA1AD))
            },
            leadingIcon = { IconoBuscar() },
            shape = RoundedCornerShape(18.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = Color.White),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Carbon,
                unfocusedContainerColor = Carbon,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = CarbonBorde,
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
                Text(
                    "${estado.ejerciciosFiltrados.size} EJERCICIOS",
                    fontFamily = BarlowCondensed,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF9AA1AD)
                )
                Spacer(Modifier.height(10.dp))
                // Cuadrícula de dos columnas; el detalle del ejercicio elegido se abre debajo de su fila.
                estado.ejerciciosFiltrados.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        fila.forEach { ejercicio ->
                            EjercicioTarjeta(
                                ejercicio = ejercicio,
                                seleccionado = ejercicio.idEjercicio == estado.ejercicioSeleccionado,
                                alPulsar = {
                                    viewModel.seleccionarEjercicio(
                                        if (ejercicio.idEjercicio == estado.ejercicioSeleccionado) null
                                        else ejercicio.idEjercicio
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (fila.size == 1) Spacer(Modifier.weight(1f))
                    }
                    fila.firstOrNull { it.idEjercicio == estado.ejercicioSeleccionado }?.let { DetalleEjercicio(it) }
                    Spacer(Modifier.height(10.dp))
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
        shape = RoundedCornerShape(20.dp),
        color = if (seleccionado) MaterialTheme.colorScheme.primary else Carbon,
        border = BorderStroke(1.dp, if (seleccionado) MaterialTheme.colorScheme.primary else CarbonBorde)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                texto.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EjercicioTarjeta(
    ejercicio: EjercicioResponse,
    seleccionado: Boolean,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = alPulsar),
        shape = RoundedCornerShape(18.dp),
        color = Carbon,
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) MaterialTheme.colorScheme.primary else CarbonBorde)
    ) {
        Column {
            IlustracionEjercicio(
                nombre = ejercicio.nombre,
                grupoMuscular = ejercicio.grupoMuscular,
                forma = RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp),
                modifier = Modifier.fillMaxWidth().height(108.dp)
            )
            Column(Modifier.heightIn(min = 74.dp).padding(horizontal = 11.dp, vertical = 10.dp)) {
                Text(
                    ejercicio.nombre ?: "Ejercicio",
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(7.dp),
                        shape = CircleShape,
                        color = colorGrupoMuscular(ejercicio.grupoMuscular)
                    ) {}
                    Spacer(Modifier.width(5.dp))
                    Text(
                        listOfNotNull(ejercicio.grupoMuscular, ejercicio.nivelExperiencia)
                            .joinToString(" · ").ifBlank { "Sin clasificación" },
                        fontSize = 12.sp,
                        color = Color(0xFF9AA1AD),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Detalle del ejercicio elegido: ilustración grande, descripción, equipo y medición. */
@Composable
private fun DetalleEjercicio(ejercicio: EjercicioResponse) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = RoundedCornerShape(20.dp),
        color = Carbon,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column {
            IlustracionEjercicio(
                nombre = ejercicio.nombre,
                grupoMuscular = ejercicio.grupoMuscular,
                forma = RoundedCornerShape(topStart = 19.dp, topEnd = 19.dp),
                modifier = Modifier.fillMaxWidth().aspectRatio(1.5f)
            )
            Column(Modifier.padding(16.dp)) {
                Text(
                    ejercicio.nombre ?: "Ejercicio",
                    fontFamily = BarlowCondensed,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = Color.White
                )
                ejercicio.descripcion?.takeIf(String::isNotBlank)?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, fontSize = 15.sp, lineHeight = 21.sp, color = Color(0xFFE0E2E6))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ejercicio.equipamiento?.takeIf(String::isNotBlank)?.let { DatoDetalle(it) }
                    ejercicio.tipoMedicion?.takeIf(String::isNotBlank)?.let {
                        DatoDetalle(if (it.equals("TIEMPO", true)) "Por tiempo" else "Por repeticiones")
                    }
                }
            }
        }
    }
}

@Composable
private fun DatoDetalle(texto: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF1A1C21), border = BorderStroke(1.dp, CarbonBorde)) {
        Text(texto, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp, color = Color.White, maxLines = 1)
    }
}
