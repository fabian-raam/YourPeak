package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.model.SolicitudAccesoItem
import com.ramirez.yourpeak.data.model.SolicitudAccesoResponse
import com.ramirez.yourpeak.data.repository.SolicitudAccesoRepository
import com.ramirez.yourpeak.domain.DestinoUsuario
import com.ramirez.yourpeak.ui.components.CarbonCampo
import com.ramirez.yourpeak.ui.components.EtiquetaEstado
import com.ramirez.yourpeak.ui.components.MarcaYourPeak
import com.ramirez.yourpeak.ui.components.NaranjaSube
import com.ramirez.yourpeak.ui.components.TarjetaCarbon
import com.ramirez.yourpeak.ui.components.TextoSecundario
import com.ramirez.yourpeak.ui.components.TituloSeccion
import com.ramirez.yourpeak.ui.theme.Azul
import com.ramirez.yourpeak.ui.theme.BarlowCondensed
import com.ramirez.yourpeak.ui.theme.CarbonBorde
import com.ramirez.yourpeak.ui.theme.Lima
import com.ramirez.yourpeak.ui.theme.LimaFondo
import com.ramirez.yourpeak.ui.theme.PendienteFondo
import com.ramirez.yourpeak.ui.theme.PendienteTexto
import com.ramirez.yourpeak.ui.theme.YourPeakTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RojoSuave = Color(0xFFFF8A73)
private val RojoFondo = Color(0xFF3A1E16)
private val formatoFecha = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.forLanguageTag("es-PE"))

/** Fecha y hora de Lima tal como las envía Spring (sin zona). */
internal fun fechaSolicitud(texto: String?): String =
    runCatching { LocalDateTime.parse(texto).format(formatoFecha) }.getOrDefault(texto.orEmpty())

@Composable
fun EstadoAccesoScreen(
    destino: DestinoUsuario,
    usuario: LoginResponse,
    repository: SolicitudAccesoRepository?,
    alCambiarEstado: (LoginResponse) -> Unit,
    alCerrarSesion: () -> Unit
) {
    val esCliente = destino in setOf(DestinoUsuario.CUENTA_PENDIENTE, DestinoUsuario.CUENTA_RECHAZADA, DestinoUsuario.CUENTA_INACTIVA)
    if (repository == null) {
        ContenidoEstadoCuenta(destino, EstadoCuentaUiState(usuario), esCliente, {}, {}, {}, alCerrarSesion)
        return
    }
    val viewModel: EstadoCuentaViewModel = viewModel(
        key = "estado-${usuario.idUsuario}-${usuario.estado}",
        factory = EstadoCuentaViewModel.factory(repository, usuario, esCliente)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ContenidoEstadoCuenta(
        destino = destino,
        estado = estado,
        esCliente = esCliente,
        alCambiarMensaje = viewModel::cambiarMensaje,
        alActualizar = { viewModel.actualizar(alCambiarEstado) },
        alSolicitar = { viewModel.volverASolicitar(alCambiarEstado) },
        alCerrarSesion = alCerrarSesion
    )
}

@Composable
fun ContenidoEstadoCuenta(
    destino: DestinoUsuario,
    estado: EstadoCuentaUiState,
    esCliente: Boolean,
    alCambiarMensaje: (String) -> Unit,
    alActualizar: () -> Unit,
    alSolicitar: () -> Unit,
    alCerrarSesion: () -> Unit
) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val historial = estado.solicitud?.historial.orEmpty()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { MarcaYourPeak(conLema = false) }
                    OutlinedButton(
                        onClick = alCerrarSesion,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorde)
                    ) { Text("Cerrar sesión", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
                Spacer(Modifier.height(4.dp))
                Text("MI CUENTA", color = TextoSecundario, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text(
                    if (esCliente) "MI SOLICITUD" else "HOLA, ${estado.usuario.nombre.orEmpty().uppercase()}",
                    color = Color.White,
                    fontFamily = BarlowCondensed,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    lineHeight = 42.sp
                )

                if (esCliente) {
                    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(CarbonCampo)) {
                        listOf("Estado", "Historial (${historial.size})").forEachIndexed { indice, texto ->
                            Tab(
                                selected = pestana == indice,
                                onClick = { pestana = indice },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (pestana == indice) Color.White else Color.Transparent)
                            ) {
                                Text(
                                    texto,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    color = if (pestana == indice) Color.Black else TextoSecundario,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                estado.error?.let { Text(it, color = RojoSuave, fontSize = 14.sp, lineHeight = 19.sp) }
                estado.aviso?.let { Text(it, color = Lima, fontSize = 14.sp) }

                if (pestana == 0 || !esCliente) {
                    TarjetaEstado(destino, estado, alActualizar)
                    if (destino == DestinoUsuario.CUENTA_RECHAZADA) TarjetaRechazo(estado, alCambiarMensaje, alSolicitar)
                    if (destino == DestinoUsuario.CUENTA_PENDIENTE) TarjetaMientras()
                } else {
                    Historial(historial)
                }
            }
        }
    }
}

@Composable
private fun TarjetaEstado(destino: DestinoUsuario, estado: EstadoCuentaUiState, alActualizar: () -> Unit) {
    val (etiqueta, titulo, texto) = when (destino) {
        DestinoUsuario.CUENTA_PENDIENTE -> Triple("● EN REVISIÓN", "Tu solicitud está en revisión",
            "Un administrador del gimnasio revisará tu solicitud y activará tu cuenta con un plan de membresía.")
        DestinoUsuario.CUENTA_RECHAZADA -> Triple("✕ RECHAZADA", "Tu solicitud no fue aprobada",
            "Revisa el motivo que dejó el administrador. Cuando lo resuelvas, puedes volver a enviar tu solicitud.")
        DestinoUsuario.CUENTA_INACTIVA -> Triple("● INACTIVA", "Tu cuenta está inactiva",
            "Tu último periodo terminó. La administración puede reactivarla al renovar tu membresía.")
        DestinoUsuario.INFORMACION_ADMINISTRADOR -> Triple("● ADMINISTRADOR", "Acceso de administrador",
            "Las herramientas administrativas están disponibles en la web de Your Peak.")
        else -> Triple("● SIN ACCESO", "No se pudo validar tu acceso",
            "El servidor devolvió un estado que la app no reconoce. Cierra sesión e inténtalo nuevamente.")
    }
    val rechazada = destino == DestinoUsuario.CUENTA_RECHAZADA
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    if (rechazada) listOf(Color(0xFF4A2416), Color(0xFF121316))
                    else listOf(Color(0xFF1A2A4A), Color(0xFF121316))
                )
            )
            .border(1.dp, CarbonBorde, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        EtiquetaEstado(etiqueta, if (rechazada) RojoSuave else PendienteTexto, if (rechazada) RojoFondo else PendienteFondo)
        Text(titulo.uppercase(), color = Color.White, fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 33.sp)
        Text(texto, color = Color.White.copy(alpha = 0.8f), fontSize = 14.5.sp, lineHeight = 20.sp)
        if (destino == DestinoUsuario.CUENTA_PENDIENTE || rechazada) Pasos(rechazada)
        Button(
            onClick = alActualizar,
            enabled = !estado.cargando,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Azul)
        ) {
            if (estado.cargando) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Consultar estado", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Pasos(rechazada: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            Triple("Solicitud enviada", true, false),
            Triple("Revisión del administrador", rechazada, !rechazada),
            Triple(if (rechazada) "Rechazada" else "Cuenta activa", rechazada, false)
        ).forEachIndexed { indice, (texto, hecho, actual) ->
            val fallo = rechazada && indice == 2
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = if (actual) 0.12f else 0.05f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape)
                        .background(if (fallo) NaranjaSube else if (hecho) Lima else if (actual) Azul else Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (fallo) "✕" else if (hecho) "✓" else "${indice + 1}",
                        color = if (hecho && !fallo) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text(texto, color = if (hecho || actual) Color.White else TextoSecundario, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TarjetaRechazo(estado: EstadoCuentaUiState, alCambiarMensaje: (String) -> Unit, alSolicitar: () -> Unit) {
    val solicitud = estado.solicitud
    TarjetaCarbon {
        TituloSeccion("MOTIVO DEL RECHAZO")
        Spacer(Modifier.height(10.dp))
        Text(
            estado.ultimoRechazo?.motivoRechazo ?: if (solicitud == null) "Consultando…" else "El administrador no indicó un motivo.",
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(RojoFondo).padding(12.dp),
            color = Color(0xFFFFD2C2),
            fontSize = 15.sp,
            lineHeight = 21.sp
        )
        estado.ultimoRechazo?.fechaResolucion?.let {
            Text("Revisada el ${fechaSolicitud(it)}", color = TextoSecundario, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        HorizontalDivider(color = CarbonBorde, modifier = Modifier.padding(vertical = 14.dp))
        Text("Mensaje para el administrador (opcional)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = estado.mensaje,
            onValueChange = alCambiarMensaje,
            enabled = solicitud?.puedeSolicitar == true && !estado.enviando,
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            placeholder = { Text("Ej.: Ya realicé el pago en recepción.", color = TextoSecundario) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedContainerColor = CarbonCampo, unfocusedContainerColor = CarbonCampo, disabledContainerColor = CarbonCampo,
                focusedBorderColor = Azul, unfocusedBorderColor = CarbonBorde, disabledBorderColor = CarbonBorde
            )
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(
                when {
                    solicitud == null -> ""
                    solicitud.puedeSolicitar -> "Te quedan ${solicitud.solicitudesRestantesHoy} de ${solicitud.limiteDiario} solicitudes hoy."
                    else -> "Alcanzaste el máximo de ${solicitud.limiteDiario} solicitudes por hoy. Podrás intentarlo mañana."
                },
                modifier = Modifier.weight(1f),
                color = TextoSecundario,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Text("${estado.mensaje.length}/300", color = TextoSecundario, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = alSolicitar,
            enabled = estado.puedeSolicitar,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Azul)
        ) {
            if (estado.enviando) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Volver a solicitar acceso", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun TarjetaMientras() {
    TarjetaCarbon {
        TituloSeccion("MIENTRAS TANTO")
        Spacer(Modifier.height(8.dp))
        listOf(
            "Ten a la mano tu pago" to "La administración asigna tu plan al aprobarte.",
            "Revisión en horario de atención" to "Suele resolverse el mismo día.",
            "Si te rechazan, verás el motivo aquí" to "Podrás corregirlo y volver a solicitar."
        ).forEach { (titulo, detalle) ->
            Column(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(12.dp)).background(CarbonCampo).padding(12.dp)) {
                Text(titulo, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                Text(detalle, color = TextoSecundario, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Historial(historial: List<SolicitudAccesoItem>) {
    if (historial.isEmpty()) {
        TarjetaCarbon { Text("Aún no hay solicitudes registradas.", color = TextoSecundario, fontSize = 14.sp) }
        return
    }
    TarjetaCarbon(relleno = 0.dp) {
        historial.forEachIndexed { indice, item ->
            if (indice > 0) HorizontalDivider(color = CarbonBorde, modifier = Modifier.padding(horizontal = 14.dp))
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Solicitud #${item.numero}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    when (item.estado?.uppercase()) {
                        "APROBADA" -> EtiquetaEstado("Aprobada", Lima, LimaFondo)
                        "RECHAZADA" -> EtiquetaEstado("Rechazada", RojoSuave, RojoFondo)
                        else -> EtiquetaEstado("En revisión", PendienteTexto, PendienteFondo)
                    }
                }
                Text("Enviada el ${fechaSolicitud(item.fechaSolicitud)}", color = TextoSecundario, fontSize = 12.sp)
                item.mensajeCliente?.let { Text("Tu mensaje: $it", color = Color.White.copy(alpha = 0.85f), fontSize = 13.5.sp, lineHeight = 18.sp) }
                item.motivoRechazo?.let { Text("Motivo: $it", color = Color(0xFFFFD2C2), fontSize = 13.5.sp, lineHeight = 18.sp) }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun RechazadaPreview() {
    YourPeakTheme {
        ContenidoEstadoCuenta(
            destino = DestinoUsuario.CUENTA_RECHAZADA,
            estado = EstadoCuentaUiState(
                usuario = LoginResponse(1, "Ana", "Torres", "ana@tecsup.edu.pe", "CLIENTE", "RECHAZADO"),
                solicitud = SolicitudAccesoResponse("RECHAZADO", true, 2, 3, listOf(
                    SolicitudAccesoItem(1, "RECHAZADA", null, "No se registró el pago de la inscripción.", "2026-11-25T14:36:51", "2026-11-26T09:10:00")
                ))
            ),
            esCliente = true,
            alCambiarMensaje = {}, alActualizar = {}, alSolicitar = {}, alCerrarSesion = {}
        )
    }
}
