package com.ramirez.yourpeak.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.repository.AsistenteRepository
import com.ramirez.yourpeak.ui.components.CarbonCampo
import com.ramirez.yourpeak.ui.components.TextoSecundario
import com.ramirez.yourpeak.ui.theme.Azul
import com.ramirez.yourpeak.ui.theme.BarlowCondensed
import com.ramirez.yourpeak.ui.theme.Carbon
import com.ramirez.yourpeak.ui.theme.CarbonBorde
import com.ramirez.yourpeak.ui.theme.Lima
import com.ramirez.yourpeak.ui.theme.PendienteFondo
import com.ramirez.yourpeak.ui.theme.PendienteTexto
import com.ramirez.yourpeak.ui.theme.YourPeakTheme

private val sugerencias = listOf(
    "¿Qué puedo comer antes de entrenar?",
    "Dame ideas de cenas altas en proteína",
    "¿Cómo hago una sentadilla con buena técnica?",
    "¿Cuánto debo descansar entre series?",
    "¿Cuánta agua debería tomar al día?"
)

@Composable
fun AsistenteScreen(
    nombre: String?,
    idUsuario: Long?,
    repository: AsistenteRepository,
    alVolver: () -> Unit
) {
    val viewModel: AsistenteViewModel = viewModel(
        key = "asistente-$idUsuario",
        factory = AsistenteViewModel.factory(repository, idUsuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    BackHandler(onBack = alVolver)
    ContenidoAsistente(
        nombre = nombre,
        estado = estado,
        alCambiarPregunta = viewModel::cambiarPregunta,
        alEnviar = { viewModel.enviar(it) },
        alNuevaConversacion = viewModel::nuevaConversacion,
        alVolver = alVolver
    )
}

@Composable
fun ContenidoAsistente(
    nombre: String?,
    estado: AsistenteUiState,
    alCambiarPregunta: (String) -> Unit,
    alEnviar: (String) -> Unit,
    alNuevaConversacion: () -> Unit,
    alVolver: () -> Unit
) {
    val lista = rememberLazyListState()
    LaunchedEffect(estado.mensajes.size, estado.enviando) {
        val total = estado.mensajes.size + if (estado.enviando) 1 else 0
        if (total > 0) lista.animateScrollToItem(total - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .imePadding()
    ) {
        // Encabezado
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1A2A4A), Color.Black)))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = alVolver) { Text("←", color = Color.White, fontSize = 22.sp) }
            AvatarCoach(40)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("COACH IA", color = Color.White, fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 24.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Lima))
                    Spacer(Modifier.width(6.dp))
                    Text("Alimentación y ejercicio", color = TextoSecundario, fontSize = 12.5.sp)
                }
            }
            if (estado.mensajes.isNotEmpty()) {
                TextButton(onClick = alNuevaConversacion, enabled = !estado.enviando) {
                    Text("Nueva", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        LazyColumn(
            state = lista,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (estado.mensajes.isEmpty()) {
                item { Bienvenida(nombre, estado.enviando, alEnviar) }
            }
            itemsIndexed(estado.mensajes) { _, mensaje -> Burbuja(mensaje) }
            if (estado.enviando) item { Escribiendo() }
        }

        // Entrada
        Column(
            Modifier
                .fillMaxWidth()
                .background(Carbon)
                .border(BorderStroke(1.dp, CarbonBorde))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = estado.pregunta,
                    onValueChange = alCambiarPregunta,
                    enabled = !estado.enviando,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp, max = 140.dp),
                    placeholder = { Text("Pregunta sobre alimentación o ejercicio…", color = TextoSecundario, fontSize = 14.sp) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = CarbonCampo, unfocusedContainerColor = CarbonCampo, disabledContainerColor = CarbonCampo,
                        focusedBorderColor = Azul, unfocusedBorderColor = CarbonBorde, disabledBorderColor = CarbonBorde,
                        cursorColor = Azul
                    )
                )
                Spacer(Modifier.width(8.dp))
                val habilitado = !estado.enviando && estado.pregunta.isNotBlank()
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (habilitado) Azul else Azul.copy(alpha = 0.35f))
                        .clickable(enabled = habilitado) { alEnviar(estado.pregunta) },
                    contentAlignment = Alignment.Center
                ) { Text("➤", color = Color.White, fontSize = 20.sp) }
            }
            Text(
                (estado.restantes?.let { "Te quedan $it de ${estado.limite} consultas hoy · " } ?: "") +
                    "Orientación general: no reemplaza a un médico ni a un nutricionista.",
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                color = TextoSecundario,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AvatarCoach(tamano: Int) {
    Box(
        Modifier
            .size(tamano.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Azul, Color(0xFF5D93F0)))),
        contentAlignment = Alignment.Center
    ) { Text("✦", color = Color.White, fontSize = (tamano * 0.45f).sp) }
}

@Composable
private fun Bienvenida(nombre: String?, enviando: Boolean, alEnviar: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AvatarCoach(56)
        Spacer(Modifier.height(12.dp))
        Text(
            "HOLA${nombre?.takeIf(String::isNotBlank)?.let { ", ${it.uppercase()}" } ?: ""}. ¿EN QUÉ TE AYUDO?",
            color = Color.White,
            fontFamily = BarlowCondensed,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            lineHeight = 30.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Pregúntame sobre comidas, macronutrientes, hidratación, técnica de ejercicios, rutinas o recuperación.",
            color = TextoSecundario,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        sugerencias.forEach { texto ->
            Surface(
                onClick = { alEnviar(texto) },
                enabled = !enviando,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = Carbon,
                border = BorderStroke(1.dp, CarbonBorde)
            ) {
                Text(texto, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun Burbuja(mensaje: MensajeChat) {
    val (fondo, colorTexto, borde) = when {
        mensaje.deUsuario -> Triple(Azul, Color.White, Azul)
        mensaje.error -> Triple(Color(0xFF33232B), Color(0xFFF4A7A9), Color(0xFF5A2E36))
        !mensaje.enTema -> Triple(PendienteFondo, PendienteTexto, Color(0xFF4A3F2A))
        else -> Triple(Carbon, Color.White, CarbonBorde)
    }
    val forma = RoundedCornerShape(
        topStart = 18.dp, topEnd = 18.dp,
        bottomStart = if (mensaje.deUsuario) 18.dp else 6.dp,
        bottomEnd = if (mensaje.deUsuario) 6.dp else 18.dp
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mensaje.deUsuario) Arrangement.End else Arrangement.Start) {
        Text(
            mensaje.texto,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(forma)
                .background(fondo)
                .border(1.dp, borde, forma)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            color = colorTexto,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun Escribiendo() {
    Row(
        Modifier.clip(RoundedCornerShape(18.dp)).background(Carbon).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(Modifier.size(14.dp), color = TextoSecundario, strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text("Escribiendo…", color = TextoSecundario, fontSize = 13.sp)
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AsistentePreview() {
    YourPeakTheme {
        ContenidoAsistente(
            nombre = "Lucía",
            estado = AsistenteUiState(
                mensajes = listOf(
                    MensajeChat(true, "¿Qué ceno después de entrenar?"),
                    MensajeChat(false, "Prueba pollo con arroz y verduras:\n- 25–35 g de proteína\n- Una porción de carbohidrato"),
                    MensajeChat(true, "¿Quién ganó el partido?"),
                    MensajeChat(false, "Solo puedo ayudarte con temas de alimentación y ejercicio.", enTema = false)
                ),
                restantes = 27, limite = 30
            ),
            alCambiarPregunta = {}, alEnviar = {}, alNuevaConversacion = {}, alVolver = {}
        )
    }
}
