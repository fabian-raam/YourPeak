package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.repository.MembresiaClienteRepository

private val NegroMembresia = Color.Black
private val BordeMembresia = Color(0xFF383838)
private val TextoSuaveMembresia = Color(0xFFB8BCC6)
private val AzulMembresia = Color(0xFF2878FF)
private val VerdeMembresia = Color(0xFFB7F34A)
private val RojoMembresia = Color(0xFFFF5656)

@Composable
fun MiMembresiaScreen(usuario: LoginResponse, repository: MembresiaClienteRepository) {
    val viewModel: MiMembresiaViewModel = viewModel(
        key = "mi-membresia-${usuario.idUsuario}",
        factory = MiMembresiaViewModel.factory(repository, usuario.idUsuario, usuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val acentoVigencia = when (estado.membresiaVigente) {
        true -> VerdeMembresia
        false -> RojoMembresia
        null -> TextoSuaveMembresia
    }
    val estadoCuenta = when (estado.estadoCuenta.uppercase()) {
        "ACTIVO" -> "Activo"
        "INACTIVO" -> "Inactivo"
        "PENDIENTE" -> "Pendiente"
        else -> estado.estadoCuenta.ifBlank { "Sin información" }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(19.dp),
            color = NegroMembresia,
            border = BorderStroke(1.dp, BordeMembresia)
        ) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                EstadoMembresiaCelda(
                    valor = if (estado.cargando && estado.membresiaVigente == null) "…" else estado.tituloEstadoMembresia,
                    etiqueta = "Estado de la membresía",
                    acento = acentoVigencia,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.fillMaxHeight().width(1.dp).background(BordeMembresia))
                EstadoMembresiaCelda(
                    valor = estadoCuenta,
                    etiqueta = "Estado de cuenta",
                    acento = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (estado.cargando) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                CircularProgressIndicator(Modifier.size(17.dp), color = AzulMembresia, strokeWidth = 2.dp)
                Text("Consultando tu membresía…", color = TextoSuaveMembresia, fontSize = 14.sp)
            }
        }

        estado.error?.let { error ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(error, color = RojoMembresia, fontSize = 14.sp, lineHeight = 19.sp)
                TextButton(onClick = viewModel::actualizar, enabled = !estado.cargando) {
                    Text("REINTENTAR CONSULTA", color = AzulMembresia, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            "La renovación de membresías se gestiona con la administración del gimnasio.",
            color = TextoSuaveMembresia,
            fontSize = 14.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun EstadoMembresiaCelda(
    valor: String,
    etiqueta: String,
    acento: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Spacer(Modifier.fillMaxWidth().height(2.dp).background(acento))
        Column(Modifier.padding(horizontal = 16.dp, vertical = 19.dp)) {
            val tamanoValor = when {
                valor.length > 11 -> 22.sp
                valor.length > 8 -> 27.sp
                else -> 38.sp
            }
            Text(
                valor,
                color = acento,
                fontSize = tamanoValor,
                lineHeight = 43.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                etiqueta,
                color = TextoSuaveMembresia,
                fontSize = 15.sp,
                lineHeight = 19.sp
            )
        }
    }
}
