package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.repository.PerfilRepository
import com.ramirez.yourpeak.ui.components.CarbonCampo
import com.ramirez.yourpeak.ui.components.IconoEntrenamiento
import com.ramirez.yourpeak.ui.components.IconoNutricion
import com.ramirez.yourpeak.ui.components.IconoProgreso
import com.ramirez.yourpeak.ui.components.TarjetaCarbon
import com.ramirez.yourpeak.ui.components.TextoSecundario
import com.ramirez.yourpeak.ui.theme.BarlowCondensed
import com.ramirez.yourpeak.ui.theme.Carbon
import com.ramirez.yourpeak.ui.theme.CarbonBorde
import com.ramirez.yourpeak.ui.theme.Lima

private val SEXOS = listOf("HOMBRE" to "Hombre", "MUJER" to "Mujer")
private val RojoCerrar = Color(0xFFFF6B6B)

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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = alVolver)) {
            Text("‹", color = Color.White, fontSize = 32.sp, lineHeight = 34.sp, modifier = Modifier.padding(end = 10.dp))
            Text("MI PERFIL", color = Color.White, fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = 1.5.sp)
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CabeceraPerfil(usuario, estado.completado, estado.cargando)
            when {
                estado.cargando -> Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("  Cargando tu perfil…", color = TextoSecundario)
                }
                estado.errorCarga != null -> {
                    Spacer(Modifier.height(12.dp))
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
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF5A2A2D))
        ) {
            Text("CERRAR SESIÓN", color = RojoCerrar, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun CabeceraPerfil(usuario: LoginResponse, completado: Boolean, cargando: Boolean) {
    val iniciales = listOfNotNull(usuario.nombre, usuario.apellido)
        .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }.joinToString("")
    TarjetaCarbon(relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(58.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(iniciales.ifEmpty { "C" }, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(
                    "${usuario.nombre.orEmpty()} ${usuario.apellido.orEmpty()}".trim().ifEmpty { "Cliente" },
                    color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold
                )
                Text(usuario.correo.orEmpty(), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                if (!cargando) {
                    Text(
                        if (completado) "✓ Perfil completo" else "Completa tu perfil para personalizar tu rutina",
                        color = if (completado) Lima else TextoSecundario,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormularioPerfil(estado: PerfilUiState, viewModel: PerfilViewModel) {
    val habilitado = !estado.guardando

    Etiqueta("OBJETIVO")
    Row(Modifier.selectableGroup().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        estado.objetivos.forEach { opcion ->
            TarjetaObjetivo(
                opcion = opcion,
                seleccionada = opcion.id == estado.idObjetivo,
                habilitada = habilitado,
                alElegir = { viewModel.elegirObjetivo(opcion.id) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }

    Etiqueta("EXPERIENCIA")
    Segmentado(estado.nivelesExperiencia, estado.idExperiencia, habilitado, viewModel::elegirExperiencia)
    DescripcionElegida(estado.nivelesExperiencia, estado.idExperiencia)

    Etiqueta("ACTIVIDAD")
    Segmentado(estado.nivelesActividad, estado.idActividad, habilitado, viewModel::elegirActividad)
    DescripcionElegida(estado.nivelesActividad, estado.idActividad)

    Etiqueta("DATOS FÍSICOS")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoMedida("Peso (kg)", "Ej. 70.5", estado.peso, habilitado, viewModel::cambiarPeso, Modifier.weight(1f))
        CampoMedida("Talla (cm)", "Ej. 175", estado.talla, habilitado, viewModel::cambiarTalla, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    val sexoGuardado = estado.sexoGuardado
    if (sexoGuardado != null) {
        TarjetaCarbon(relleno = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sexo", color = TextoSecundario, fontSize = 12.sp)
                    Text(
                        SEXOS.firstOrNull { it.first == sexoGuardado }?.second ?: sexoGuardado,
                        color = Color.White, fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp
                    )
                }
                Text("🔒 Queda fijo", color = TextoSecundario, fontSize = 12.sp)
            }
        }
    } else {
        Text("Sexo", color = TextoSecundario, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        Segmentado(
            SEXOS.mapIndexed { i, (_, etiqueta) -> OpcionPerfil(i, etiqueta, "") },
            SEXOS.indexOfFirst { it.first == estado.sexo }.takeIf { it >= 0 },
            habilitado
        ) { viewModel.elegirSexo(SEXOS[it].first) }
        Text("Puedes elegirlo antes de guardar. Después quedará fijo.", color = TextoSecundario, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
    Text(
        "Si cambias tu peso o talla, se añade una medición a tu historial.",
        modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
        color = TextoSecundario,
        fontSize = 12.sp
    )
    estado.error?.let { MensajePerfil(it, error = true) }
    estado.aviso?.let { MensajePerfil(it, error = false, cerrar = viewModel::cerrarAviso) }
    Button(
        onClick = viewModel::guardar,
        enabled = habilitado,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (estado.guardando) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            Text("  GUARDANDO", fontWeight = FontWeight.ExtraBold)
        } else Text("GUARDAR PERFIL", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(
        texto,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        color = TextoSecundario,
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        letterSpacing = 1.5.sp
    )
}

/** Objetivo como tarjeta con ícono: perder grasa, ganar masa o mantener. */
@Composable
private fun TarjetaObjetivo(
    opcion: OpcionPerfil,
    seleccionada: Boolean,
    habilitada: Boolean,
    alElegir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primario = MaterialTheme.colorScheme.primary
    val color = if (seleccionada) primario else TextoSecundario
    Surface(
        modifier = modifier.selectable(selected = seleccionada, enabled = habilitada, role = Role.RadioButton, onClick = alElegir),
        shape = RoundedCornerShape(16.dp),
        color = if (seleccionada) primario.copy(alpha = 0.14f) else Carbon,
        border = BorderStroke(1.dp, if (seleccionada) primario else CarbonBorde)
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val nombre = opcion.nombre.lowercase()
            val icono = Modifier.size(24.dp)
            when {
                "perder" in nombre -> IconoNutricion(color, icono)
                "ganar" in nombre -> IconoEntrenamiento(color, icono)
                else -> IconoProgreso(color, icono)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                opcion.nombre,
                color = Color.White,
                fontSize = 12.5.sp,
                lineHeight = 15.sp,
                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/** Botones segmentados en una sola fila para elegir una opción del catálogo. */
@Composable
private fun Segmentado(
    opciones: List<OpcionPerfil>,
    seleccionada: Int?,
    habilitado: Boolean,
    alElegir: (Int) -> Unit
) {
    Surface(shape = RoundedCornerShape(14.dp), color = Carbon, border = BorderStroke(1.dp, CarbonBorde)) {
        Row(Modifier.fillMaxWidth().padding(3.dp).selectableGroup()) {
            opciones.forEach { opcion ->
                val elegida = opcion.id == seleccionada
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (elegida) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .selectable(selected = elegida, enabled = habilitado, role = Role.RadioButton) { alElegir(opcion.id) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        opcion.nombre,
                        color = if (elegida) Color.White else TextoSecundario,
                        fontSize = 12.5.sp,
                        fontWeight = if (elegida) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DescripcionElegida(opciones: List<OpcionPerfil>, seleccionada: Int?) {
    opciones.firstOrNull { it.id == seleccionada }?.descripcion?.takeIf(String::isNotBlank)?.let {
        Text(it, color = TextoSecundario, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
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
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Carbon,
            unfocusedContainerColor = Carbon,
            disabledContainerColor = CarbonCampo,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = CarbonBorde
        )
    )
}

@Composable
private fun MensajePerfil(texto: String, error: Boolean, cerrar: (() -> Unit)? = null) {
    val acento = if (error) RojoCerrar else Lima
    Box(Modifier.padding(bottom = 12.dp)) {
        TarjetaCarbon(borde = acento.copy(alpha = 0.55f), relleno = 4.dp) {
            Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("●", color = acento, fontSize = 14.sp)
                Text(texto, modifier = Modifier.weight(1f).padding(horizontal = 9.dp, vertical = 8.dp), color = Color.White, fontSize = 14.sp, lineHeight = 18.sp)
                cerrar?.let { TextButton(onClick = it) { Text("Cerrar", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp) } }
            }
        }
    }
}
