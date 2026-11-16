package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.ui.components.MarcaYourPeak

@Composable
fun MenuLateralCliente(
    usuario: LoginResponse,
    seccionSeleccionada: SeccionCliente,
    alSeleccionar: (SeccionCliente) -> Unit,
    alCerrarSesion: () -> Unit
) {
    ModalDrawerSheet {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
            MarcaYourPeak(conLema = false)
            Spacer(Modifier.height(24.dp))
            Text(
                text = listOfNotNull(usuario.nombre, usuario.apellido).joinToString(" "),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = usuario.correo.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        NavigationDrawerItem(
            label = { Text(SeccionCliente.INICIO.titulo) },
            selected = seccionSeleccionada == SeccionCliente.INICIO,
            onClick = { alSeleccionar(SeccionCliente.INICIO) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Text(
            text = "Entrenamiento",
            modifier = Modifier.padding(start = 28.dp, top = 14.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        listOf(SeccionCliente.MI_RUTINA, SeccionCliente.EJERCICIOS, SeccionCliente.DISPONIBILIDAD)
            .forEach { seccion ->
                NavigationDrawerItem(
                    label = { Text(seccion.titulo) },
                    selected = seccionSeleccionada == seccion,
                    onClick = { alSeleccionar(seccion) },
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp)
                )
            }
        listOf(SeccionCliente.NUTRICION, SeccionCliente.PROGRESO, SeccionCliente.MEMBRESIA)
            .forEach { seccion ->
                NavigationDrawerItem(
                    label = { Text(seccion.titulo) },
                    selected = seccionSeleccionada == seccion,
                    onClick = { alSeleccionar(seccion) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        NavigationDrawerItem(
            label = { Text("Cerrar sesión") },
            selected = false,
            onClick = alCerrarSesion,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
