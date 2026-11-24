package com.ramirez.yourpeak.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.repository.PerfilRepository
import com.ramirez.yourpeak.ui.components.IconoPerfil

private val SEXOS = listOf("HOMBRE" to "Hombre", "MUJER" to "Mujer")

/** Equivalente móvil de «Mi perfil» en la web: datos de la cuenta y perfil inicial editable. */
@Composable
fun PerfilScreen(
    usuario: LoginResponse,
    repository: PerfilRepository,
    alCerrarSesion: () -> Unit,
    alVolver: () -> Unit
) {
    val viewModel: PerfilViewModel = viewModel(
        key = "perfil-${usuario.idUsuario}",
        factory = PerfilViewModel.factory(repository, usuario.idUsuario)
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "‹",
                modifier = Modifier.clickable(onClick = alVolver).padding(end = 10.dp),
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "YOUR PEAK",
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            text = buildAnnotatedString {
                append("PERFIL")
                withStyle(SpanStyle(color = Color(0xFF2878FF))) { append(".") }
            },
            color = Color.White,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.2).sp
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(Color(0xFF141414), CircleShape)
                    .border(1.dp, Color(0xFF333333), CircleShape),
                contentAlignment = Alignment.Center
            ) { IconoPerfil(Color.White, Modifier.size(27.dp)) }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "${usuario.nombre.orEmpty()} ${usuario.apellido.orEmpty()}".trim().ifEmpty { "Cliente" },
                    color = Color.White,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    usuario.correo.orEmpty(),
                    color = Color(0xFF83AEFF),
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
        ) {
            TarjetaPerfil {
            Text(
                if (estado.completado) "Perfil completo" else "Completa tu perfil",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Tus datos de entrenamiento ajustan tu rutina con IA y tu meta nutricional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            }
            Spacer(Modifier.height(12.dp))

            when {
            estado.cargando -> Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("  Cargando tu perfil…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            estado.errorCarga != null -> {
                MensajePerfil(estado.errorCarga.orEmpty(), error = true)
                TextButton(onClick = viewModel::cargar) { Text("Reintentar") }
            }
                else -> FormularioPerfil(estado, viewModel)
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = alCerrarSesion,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFF713B3E))
        ) {
            Text(
                "CERRAR SESIÓN",
                color = Color(0xFFFF5A63),
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.45.sp
            )
        }
    }
}

@Composable
private fun FormularioPerfil(estado: PerfilUiState, viewModel: PerfilViewModel) {
    val habilitado = !estado.guardando
    TarjetaPerfil {
        TituloSeccion("Entrenamiento")
        GrupoOpciones("Objetivo", estado.objetivos, estado.idObjetivo, habilitado, viewModel::elegirObjetivo)
        GrupoOpciones("Nivel de experiencia", estado.nivelesExperiencia, estado.idExperiencia, habilitado, viewModel::elegirExperiencia)
        GrupoOpciones("Nivel de actividad", estado.nivelesActividad, estado.idActividad, habilitado, viewModel::elegirActividad)
    }
    Spacer(Modifier.height(12.dp))
    TarjetaPerfil {
        TituloSeccion("Datos físicos")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoMedida("Peso (kg)", "Ej. 70.5", estado.peso, habilitado, viewModel::cambiarPeso, Modifier.weight(1f))
            CampoMedida("Talla (cm)", "Ej. 175", estado.talla, habilitado, viewModel::cambiarTalla, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Text("Sexo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val sexoGuardado = estado.sexoGuardado
        if (sexoGuardado != null) {
            Opcion(
                titulo = SEXOS.firstOrNull { it.first == sexoGuardado }?.second ?: sexoGuardado,
                descripcion = "Este dato queda fijo después de guardarlo.",
                seleccionada = true,
                habilitada = false,
                alElegir = {}
            )
        } else {
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SEXOS.forEach { (valor, etiqueta) ->
                    Opcion(
                        titulo = etiqueta,
                        descripcion = null,
                        seleccionada = estado.sexo == valor,
                        habilitada = habilitado,
                        alElegir = { viewModel.elegirSexo(valor) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                "Puedes elegirlo antes de guardar. Después quedará fijo.",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "Si cambias tu peso o talla, se añade una medición a tu historial.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(12.dp))
    estado.error?.let { MensajePerfil(it, error = true) }
    estado.aviso?.let { MensajePerfil(it, error = false, cerrar = viewModel::cerrarAviso) }
    Button(
        onClick = viewModel::guardar,
        enabled = habilitado,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        if (estado.guardando) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            Text("  GUARDANDO", fontWeight = FontWeight.Black)
        } else Text("GUARDAR PERFIL", fontWeight = FontWeight.Black)
    }
}

@Composable
private fun TarjetaPerfil(contenido: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) { contenido() }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun GrupoOpciones(
    titulo: String,
    opciones: List<OpcionPerfil>,
    seleccionada: Int?,
    habilitado: Boolean,
    alElegir: (Int) -> Unit
) {
    Text(titulo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { opcion ->
            Opcion(
                titulo = opcion.nombre,
                descripcion = opcion.descripcion.ifBlank { null },
                seleccionada = opcion.id == seleccionada,
                habilitada = habilitado,
                alElegir = { alElegir(opcion.id) }
            )
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun Opcion(
    titulo: String,
    descripcion: String?,
    seleccionada: Boolean,
    habilitada: Boolean,
    alElegir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primario = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = seleccionada, enabled = habilitada, role = Role.RadioButton, onClick = alElegir),
        shape = RoundedCornerShape(14.dp),
        color = if (seleccionada) primario.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (seleccionada) primario else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                color = if (seleccionada) primario else MaterialTheme.colorScheme.onSurface
            )
            descripcion?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CampoMedida(
    etiqueta: String,
    ejemplo: String,
    valor: String,
    habilitado: Boolean,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        modifier = modifier,
        label = { Text(etiqueta) },
        placeholder = { Text(ejemplo) },
        singleLine = true,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun MensajePerfil(texto: String, error: Boolean, cerrar: (() -> Unit)? = null) {
    val color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(texto, modifier = Modifier.weight(1f).padding(vertical = 10.dp), color = color, style = MaterialTheme.typography.bodySmall)
            cerrar?.let { TextButton(onClick = it) { Text("×", color = color) } }
        }
    }
}
