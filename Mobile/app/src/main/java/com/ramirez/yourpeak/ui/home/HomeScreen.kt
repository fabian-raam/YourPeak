package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.ui.components.Aviso
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.components.FondoMarca
import com.ramirez.yourpeak.ui.components.MarcaYourPeak
import com.ramirez.yourpeak.ui.components.NotaPie
import com.ramirez.yourpeak.ui.components.TarjetaAuth
import com.ramirez.yourpeak.ui.components.TipoAviso
import com.ramirez.yourpeak.ui.theme.LocalEstadoColors
import com.ramirez.yourpeak.ui.theme.YourPeakTheme
import java.util.Locale

@Composable
fun HomeScreen(usuario: LoginResponse, alCerrarSesion: () -> Unit) {
    val estado = usuario.estado?.uppercase(Locale.ROOT)

    FondoMarca(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 520.dp)) {
                MarcaYourPeak(Modifier.padding(start = 6.dp, top = 8.dp), conLema = false)
                Spacer(Modifier.height(28.dp))

                TarjetaAuth {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(usuario)
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "YOUR PEAK · MI CUENTA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            EtiquetaEstado(estado, usuario.rol)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    EncabezadoTarjeta(
                        etiqueta = null,
                        titulo = "Hola, ${usuario.nombre.orEmpty()}",
                        introduccion = "Tu próxima meta empieza aquí."
                    )

                    when (estado) {
                        "PENDIENTE" -> Aviso(
                            "Tu cuenta está pendiente de aprobación. Un administrador del gimnasio la revisará pronto.",
                            TipoAviso.Pendiente
                        )
                        "INACTIVO" -> Aviso(
                            "Tu cuenta está inactiva. Acércate a recepción para reactivarla.",
                            TipoAviso.Error
                        )
                        "ACTIVO" -> Aviso("Tu cuenta está activa. ¡A entrenar!", TipoAviso.Exito)
                    }

                    DatoPerfil("Nombre completo", listOfNotNull(usuario.nombre, usuario.apellido).joinToString(" "))
                    DatoPerfil("Correo institucional", usuario.correo.orEmpty())
                    DatoPerfil("Rol", usuario.rol?.let(::capitalizar) ?: "Sin rol asignado")
                    DatoPerfil("Estado", estado?.let(::capitalizar) ?: "—", ultimo = true)

                    Spacer(Modifier.height(22.dp))
                    OutlinedButton(
                        onClick = alCerrarSesion,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge)
                    }
                }
                NotaPie()
            }
        }
    }
}

@Composable
private fun Avatar(usuario: LoginResponse) {
    val iniciales = listOfNotNull(usuario.nombre, usuario.apellido)
        .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "YP" }
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iniciales,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun EtiquetaEstado(estado: String?, rol: String?) {
    val colores = LocalEstadoColors.current
    val (texto, color, fondo) = when {
        rol == "ADMINISTRADOR" -> Triple("Administrador", MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer)
        estado == "ACTIVO" -> Triple("Activo", colores.exito, colores.exitoFondo)
        estado == "PENDIENTE" -> Triple("Pendiente", colores.pendiente, colores.pendienteFondo)
        estado == "INACTIVO" -> Triple("Inactivo", MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.errorContainer)
        else -> Triple("Sin estado", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    }
    Row(
        modifier = Modifier
            .background(fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(texto, color = color, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun DatoPerfil(etiqueta: String, valor: String, ultimo: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(valor, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
    if (!ultimo) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private fun capitalizar(texto: String) =
    texto.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun HomePreview() {
    YourPeakTheme {
        HomeScreen(
            usuario = LoginResponse(1, "Ana", "Torres", "ana.torres@tecsup.edu.pe", "CLIENTE", "PENDIENTE"),
            alCerrarSesion = {}
        )
    }
}

