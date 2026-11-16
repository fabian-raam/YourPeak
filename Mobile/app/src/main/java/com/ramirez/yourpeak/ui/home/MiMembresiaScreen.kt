package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.repository.MembresiaClienteRepository
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta

@Composable
fun MiMembresiaScreen(usuario: LoginResponse, repository: MembresiaClienteRepository) {
    val viewModel: MiMembresiaViewModel = viewModel(
        key = "mi-membresia-${usuario.idUsuario}",
        factory = MiMembresiaViewModel.factory(repository, usuario.idUsuario, usuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val vigente = estado.membresiaVigente
    val colorEstado = when (vigente) {
        true -> MaterialTheme.colorScheme.tertiary
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column {
        EncabezadoTarjeta(
            etiqueta = "CUENTA DEL CLIENTE",
            titulo = "Mi membresía",
            introduccion = "Consulta si tu membresía está vigente."
        )
        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Estado de la membresía", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Tu acceso actual a Your Peak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorEstado.copy(alpha = 0.12f)
                    ) {
                        Text(
                            if (estado.cargando && estado.membresiaVigente == null) "Actualizando…"
                            else estado.tituloEstadoMembresia,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = colorEstado,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                if (estado.cargando) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.height(16.dp), strokeWidth = 2.dp)
                        Text(
                            " Consultando tu membresía…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                estado.error?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = viewModel::actualizar, enabled = !estado.cargando) {
                        Text("Reintentar consulta")
                    }
                }
                if (estado.error != null) Spacer(Modifier.height(8.dp))
                Text(
                    estado.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(18.dp))
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(14.dp))
                Text("Estado de cuenta", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    estado.estadoCuenta.ifBlank { "No disponible" },
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "La renovación de membresías se gestiona con la administración del gimnasio.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!estado.cargando) {
            TextButton(onClick = viewModel::actualizar) { Text("Actualizar estado") }
        }
    }
}
