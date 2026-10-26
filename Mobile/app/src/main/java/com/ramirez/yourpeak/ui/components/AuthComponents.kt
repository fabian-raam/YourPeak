package com.ramirez.yourpeak.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramirez.yourpeak.R
import com.ramirez.yourpeak.ui.theme.LocalEstadoColors

/** Fondo suave con un degradado rosado en la parte superior. */
@Composable
fun FondoMarca(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0f to colores.primaryContainer,
                0.45f to colores.background,
                1f to colores.background
            )
        )
    ) { content() }
}

@Composable
fun MarcaYourPeak(modifier: Modifier = Modifier, tamanoLogo: Int = 30, conLema: Boolean = true) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(width = (tamanoLogo * 0.6f).dp, height = tamanoLogo.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "YOUR PEAK",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (tamanoLogo * 0.66f).sp,
                letterSpacing = 1.sp
            )
        }
        if (conLema) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "TU MEJOR VERSIÓN EMPIEZA AQUÍ.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Tarjeta principal con una animación de entrada como en la web. */
@Composable
fun TarjetaAuth(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(450)) + slideInVertically(tween(500)) { it / 10 }
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
                content = content
            )
        }
    }
}

@Composable
fun EncabezadoTarjeta(etiqueta: String?, titulo: String, introduccion: String) {
    if (!etiqueta.isNullOrEmpty()) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.height(10.dp))
    }
    Text(
        text = titulo,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = introduccion,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(22.dp))
}

enum class TipoAviso { Error, Exito, Pendiente }

@Composable
fun Aviso(mensaje: String?, tipo: TipoAviso, modifier: Modifier = Modifier) {
    val estados = LocalEstadoColors.current
    val (texto, fondo) = when (tipo) {
        TipoAviso.Error -> MaterialTheme.colorScheme.onErrorContainer to MaterialTheme.colorScheme.errorContainer
        TipoAviso.Exito -> estados.exito to estados.exitoFondo
        TipoAviso.Pendiente -> estados.pendiente to estados.pendienteFondo
    }
    AnimatedVisibility(visible = mensaje != null, enter = fadeIn(tween(200))) {
        Text(
            text = mensaje.orEmpty(),
            color = texto,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
                .background(fondo, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    @DrawableRes icono: Int?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    ayuda: String? = null,
    esContrasena: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    habilitado: Boolean = true,
    soloLectura: Boolean = false,
    alPresionar: (() -> Unit)? = null,
    longitudMaxima: Int = Int.MAX_VALUE,
    accionesTeclado: KeyboardActions = KeyboardActions.Default,
    ultimo: Boolean = false
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val colores = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
            color = colores.onSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box {
            OutlinedTextField(
                value = valor,
                onValueChange = { if (it.length <= longitudMaxima) alCambiar(it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = habilitado,
                readOnly = soloLectura,
                singleLine = true,
                isError = error != null,
                placeholder = if (placeholder != null) {
                    { Text(placeholder) }
                } else null,
                leadingIcon = if (icono != null) {
                    { Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(20.dp)) }
                } else null,
                trailingIcon = if (esContrasena) {
                    {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                painter = painterResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                                contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else null,
                visualTransformation = if (esContrasena && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (esContrasena) KeyboardType.Password else tipoTeclado,
                    imeAction = if (ultimo) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = accionesTeclado,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colores.surfaceVariant,
                    unfocusedContainerColor = colores.surfaceVariant,
                    disabledContainerColor = colores.surfaceVariant,
                    errorContainerColor = colores.surfaceVariant,
                    focusedBorderColor = colores.tertiary,
                    unfocusedBorderColor = colores.outline,
                    errorBorderColor = colores.error,
                    focusedLeadingIconColor = colores.tertiary,
                    unfocusedLeadingIconColor = colores.onSurfaceVariant,
                    focusedTrailingIconColor = colores.onSurfaceVariant,
                    unfocusedTrailingIconColor = colores.onSurfaceVariant,
                    cursorColor = colores.tertiary,
                    unfocusedPlaceholderColor = colores.onSurfaceVariant.copy(alpha = 0.7f),
                    focusedPlaceholderColor = colores.onSurfaceVariant.copy(alpha = 0.7f),
                    disabledTextColor = colores.onSurface,
                    disabledBorderColor = colores.outline,
                    disabledLeadingIconColor = colores.onSurfaceVariant
                )
            )
            // Capa transparente para campos que abren un selector (fecha).
            if (alPresionar != null && habilitado) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .then(Modifier.clickableSinIndicacion(alPresionar))
                )
            }
        }
        val textoInferior = error ?: ayuda
        if (textoInferior != null) {
            Text(
                text = textoInferior,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = if (error != null) colores.error else colores.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun BotonPrincipal(
    texto: String,
    alPresionar: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false
) {
    Button(
        onClick = alPresionar,
        enabled = !cargando,
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(10.dp))
            Text("Un momento…", style = MaterialTheme.typography.labelLarge)
        } else {
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun EnlaceCambio(pregunta: String, accion: String, alPresionar: () -> Unit, habilitado: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(pregunta, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = alPresionar, enabled = habilitado) {
            Text(accion, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun NotaPie(modifier: Modifier = Modifier) {
    Text(
        text = "Entrena con propósito. Supera tus límites.",
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(top = 20.dp),
        textAlign = TextAlign.Center
    )
}
