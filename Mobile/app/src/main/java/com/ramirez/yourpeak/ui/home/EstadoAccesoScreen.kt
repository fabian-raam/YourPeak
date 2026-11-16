package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.domain.DestinoUsuario
import com.ramirez.yourpeak.ui.components.Aviso
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.components.FondoMarca
import com.ramirez.yourpeak.ui.components.MarcaYourPeak
import com.ramirez.yourpeak.ui.components.NotaPie
import com.ramirez.yourpeak.ui.components.TarjetaAuth
import com.ramirez.yourpeak.ui.components.TipoAviso
import com.ramirez.yourpeak.ui.theme.YourPeakTheme

@Composable
fun EstadoAccesoScreen(
    destino: DestinoUsuario,
    usuario: LoginResponse,
    alCerrarSesion: () -> Unit
) {
    val contenido = when (destino) {
        DestinoUsuario.CUENTA_PENDIENTE -> Triple(
            "Cuenta pendiente de validación",
            "Un administrador debe revisar tu registro. Cuando tu cuenta sea aprobada, podrás acceder a las funciones del gimnasio.",
            TipoAviso.Pendiente
        )
        DestinoUsuario.CUENTA_INACTIVA -> Triple(
            "Cuenta inactiva",
            "Tu cuenta no tiene acceso a las funciones del gimnasio. Comunícate con la administración para recibir ayuda.",
            TipoAviso.Error
        )
        DestinoUsuario.INFORMACION_ADMINISTRADOR -> Triple(
            "Acceso de administrador",
            "Tu cuenta fue reconocida como administrador. Las herramientas administrativas todavía no están disponibles en esta app móvil.",
            TipoAviso.Exito
        )
        else -> Triple(
            "No se pudo validar tu acceso",
            "El servidor devolvió un rol o estado que la app todavía no reconoce. Cierra sesión e inténtalo nuevamente.",
            TipoAviso.Error
        )
    }

    FondoMarca(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(Modifier.widthIn(max = 520.dp)) {
                MarcaYourPeak(conLema = false)
                Spacer(Modifier.height(24.dp))
                TarjetaAuth {
                    EncabezadoTarjeta(
                        etiqueta = "YOUR PEAK · MI CUENTA",
                        titulo = "Hola, ${usuario.nombre.orEmpty()}",
                        introduccion = contenido.first
                    )
                    Aviso(contenido.second, contenido.third)
                    OutlinedButton(
                        onClick = alCerrarSesion,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(11.dp)
                    ) {
                        Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
                    }
                }
                NotaPie()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PendientePreview() {
    YourPeakTheme {
        EstadoAccesoScreen(
            destino = DestinoUsuario.CUENTA_PENDIENTE,
            usuario = LoginResponse(1, "Ana", "Torres", "ana@tecsup.edu.pe", "CLIENTE", "PENDIENTE"),
            alCerrarSesion = {}
        )
    }
}
