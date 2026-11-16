package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.EjercicioResponse
import com.ramirez.yourpeak.data.repository.EjercicioRepository
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta

@Composable
fun EjerciciosScreen(idUsuario: Long?, repository: EjercicioRepository) {
    val viewModel: EjerciciosViewModel = viewModel(
        key = "catalogo-ejercicios-$idUsuario",
        factory = EjerciciosViewModel.factory(repository, idUsuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column {
        EncabezadoTarjeta(
            etiqueta = "ENTRENAMIENTO",
            titulo = "Ejercicios",
            introduccion = "Explora los ejercicios activos del gimnasio."
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = estado.busqueda,
            onValueChange = viewModel::cambiarBusqueda,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Buscar por ejercicio, grupo o equipo") }
        )

        if (estado.grupos.isNotEmpty()) {
            Text(
                "Grupo muscular",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            FiltrosHorizontales(
                opciones = estado.grupos,
                seleccionado = estado.grupoSeleccionado,
                alSeleccionar = viewModel::cambiarGrupo
            )
        }

        if (estado.niveles.isNotEmpty()) {
            Text(
                "Nivel",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            FiltrosHorizontales(
                opciones = estado.niveles,
                seleccionado = estado.nivelSeleccionado,
                alSeleccionar = viewModel::cambiarNivel
            )
        }

        Spacer(Modifier.height(12.dp))
        when {
            estado.cargando -> CircularProgressIndicator()
            estado.error != null -> {
                Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::reintentar) { Text("Reintentar") }
            }
            estado.ejerciciosFiltrados.isEmpty() -> Text(
                if (estado.ejercicios.isEmpty()) "Aún no hay ejercicios activos en el catálogo."
                else "No hay ejercicios que coincidan con la búsqueda y los filtros.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> {
                Text(
                    "${estado.ejerciciosFiltrados.size} ejercicios",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                estado.ejerciciosFiltrados.forEach { ejercicio ->
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
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
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
        FilterChip(
            selected = seleccionado == null,
            onClick = { alSeleccionar(null) },
            label = { Text("Todos") }
        )
        opciones.forEach { opcion ->
            FilterChip(
                selected = seleccionado == opcion,
                onClick = { alSeleccionar(if (seleccionado == opcion) null else opcion) },
                label = { Text(opcion) }
            )
        }
    }
}

@Composable
private fun EjercicioTarjeta(ejercicio: EjercicioResponse, expandido: Boolean, alPulsar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = alPulsar),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                ejercicio.nombre ?: "Ejercicio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                listOfNotNull(ejercicio.grupoMuscular, ejercicio.nivelExperiencia)
                    .joinToString(" · ").ifBlank { "Sin clasificación" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (expandido) {
                ejercicio.descripcion?.takeIf(String::isNotBlank)?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                ejercicio.equipamiento?.takeIf(String::isNotBlank)?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("Equipo: $it", style = MaterialTheme.typography.bodySmall)
                }
                ejercicio.tipoMedicion?.takeIf(String::isNotBlank)?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("Medición: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
