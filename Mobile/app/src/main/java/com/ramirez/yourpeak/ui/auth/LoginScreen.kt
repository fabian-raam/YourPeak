package com.ramirez.yourpeak.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.R
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.ui.components.Aviso
import com.ramirez.yourpeak.ui.components.TipoAviso
import com.ramirez.yourpeak.ui.theme.Azul
import com.ramirez.yourpeak.ui.theme.Campo
import com.ramirez.yourpeak.ui.theme.CampoBorde
import com.ramirez.yourpeak.ui.theme.Texto
import com.ramirez.yourpeak.ui.theme.TextoPlaceholder
import com.ramirez.yourpeak.ui.theme.TextoSuave
import com.ramirez.yourpeak.ui.theme.YourPeakTheme

@Composable
fun LoginScreen(
    correoInicial: String?,
    avisoInicial: String?,
    alIniciarSesion: (LoginResponse) -> Unit,
    alIrARegistro: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(correoInicial, avisoInicial) {
        viewModel.prepararDesdeRegistro(correoInicial, avisoInicial)
    }
    LaunchedEffect(estado.usuarioAutenticado) {
        estado.usuarioAutenticado?.let {
            alIniciarSesion(it)
            viewModel.navegacionRealizada()
        }
    }

    LoginContenido(
        estado = estado,
        alCambiarCorreo = viewModel::cambiarCorreo,
        alCambiarContrasena = viewModel::cambiarContrasena,
        alEnviar = viewModel::iniciarSesion,
        alIrARegistro = alIrARegistro
    )
}

@Composable
private fun LoginContenido(
    estado: LoginUiState,
    alCambiarCorreo: (String) -> Unit,
    alCambiarContrasena: (String) -> Unit,
    alEnviar: () -> Unit,
    alIrARegistro: () -> Unit
) {
    val foco = LocalFocusManager.current
    var mostrarContrasena by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        val alturaMaxima = maxHeight
        Image(
            painter = painterResource(R.drawable.athlete_login),
            contentDescription = null,
            alignment = Alignment.TopCenter,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(maxHeight * 0.66f)
                .alpha(0.5f)
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.35f to Color.Black.copy(alpha = 0.08f),
                    0.68f to Color.Black.copy(alpha = 0.92f),
                    0.82f to Color.Black,
                    1f to Color.Black
                )
            )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "YOUR PEAK",
                color = Texto,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(alturaMaxima * 0.18f))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("INICIAR SESI\u00D3N") }
                    withStyle(SpanStyle(color = Azul)) { append(".") }
                },
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 42.sp,
                lineHeight = 46.sp,
                letterSpacing = (-1.4).sp,
                maxLines = 1
            )
            Text(
                text = "Bienvenido de nuevo a Your Peak.",
                color = TextoSuave,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp)
            )

            Spacer(Modifier.height(22.dp))
            Aviso(estado.aviso, TipoAviso.Exito)
            Aviso(estado.mensajeError, TipoAviso.Error)

            CampoAutenticacion(
                valor = estado.correo,
                alCambiar = alCambiarCorreo,
                etiqueta = "Correo institucional",
                icono = R.drawable.ic_mail,
                placeholder = "nombre@tecsup.edu.pe",
                ayuda = "Acceso exclusivo con tu correo @tecsup.edu.pe.",
                error = estado.errores["correo"],
                tipoTeclado = KeyboardType.Email,
                habilitado = !estado.cargando,
                longitudMaxima = 254
            )
            Spacer(Modifier.height(14.dp))
            CampoAutenticacion(
                valor = estado.contrasena,
                alCambiar = alCambiarContrasena,
                etiqueta = "Contrase\u00F1a",
                icono = R.drawable.ic_lock,
                placeholder = "Ingresa tu contrase\u00F1a",
                error = estado.errores["contrasena"],
                esContrasena = true,
                contrasenaVisible = mostrarContrasena,
                alCambiarVisibilidad = { mostrarContrasena = !mostrarContrasena },
                habilitado = !estado.cargando,
                longitudMaxima = 128,
                ultimo = true,
                alEnviar = { foco.clearFocus(); alEnviar() }
            )

            Spacer(Modifier.height(12.dp))

            BotonAutenticacion(
                texto = "INICIAR SESI\u00D3N",
                textoEnProceso = "INICIANDO SESI\u00D3N...",
                alPresionar = { foco.clearFocus(); alEnviar() },
                cargando = estado.cargando
            )

            Spacer(Modifier.height((alturaMaxima * 0.22f).coerceAtLeast(52.dp)))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u00BFNo tienes una cuenta?",
                    color = TextoSuave,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
                )
                TextButton(onClick = alIrARegistro, enabled = !estado.cargando) {
                    Text(
                        text = "Reg\u00EDstrate",
                        color = Azul,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BotonAutenticacion(
    texto: String,
    textoEnProceso: String,
    alPresionar: () -> Unit,
    cargando: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = alPresionar,
        enabled = !cargando,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Azul,
            contentColor = Color.White,
            disabledContainerColor = Azul.copy(alpha = 0.6f),
            disabledContentColor = Color.White
        )
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (cargando) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
            }
            Text(
                text = if (cargando) textoEnProceso else texto,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun CampoAutenticacion(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    icono: Int?,
    placeholder: String,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    error: String? = null,
    esContrasena: Boolean = false,
    contrasenaVisible: Boolean = false,
    alCambiarVisibilidad: () -> Unit = {},
    tipoTeclado: KeyboardType = KeyboardType.Text,
    habilitado: Boolean = true,
    longitudMaxima: Int,
    alEnviar: () -> Unit = {},
    soloLectura: Boolean = false,
    alPresionar: (() -> Unit)? = null,
    ultimo: Boolean = esContrasena
) {
    Column(modifier.fillMaxWidth().padding(bottom = 11.dp)) {
        Text(
            text = etiqueta,
            color = Color(0xFFE2E7F0),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box {
        OutlinedTextField(
            value = valor,
            onValueChange = { if (it.length <= longitudMaxima) alCambiar(it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            enabled = habilitado,
            readOnly = soloLectura,
            singleLine = true,
            isError = error != null,
            placeholder = {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp))
            },
            leadingIcon = if (icono != null) {
                { Icon(painterResource(icono), contentDescription = null) }
            } else null,
            trailingIcon = if (esContrasena) {
                {
                    IconButton(onClick = alCambiarVisibilidad, enabled = habilitado) {
                        Icon(
                            painter = painterResource(
                                if (contrasenaVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
                            ),
                            contentDescription = if (contrasenaVisible) "Ocultar contrase\u00F1a" else "Mostrar contrase\u00F1a"
                        )
                    }
                }
            } else null,
            visualTransformation = if (esContrasena && !contrasenaVisible) {
                PasswordVisualTransformation()
            } else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (esContrasena) KeyboardType.Password else tipoTeclado,
                imeAction = if (ultimo) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onDone = { alEnviar() }),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, color = Texto),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Campo,
                unfocusedContainerColor = Campo,
                disabledContainerColor = Campo,
                errorContainerColor = Campo,
                focusedBorderColor = CampoBorde,
                unfocusedBorderColor = CampoBorde,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedTextColor = Texto,
                unfocusedTextColor = Texto,
                disabledTextColor = TextoSuave,
                focusedPlaceholderColor = TextoPlaceholder,
                unfocusedPlaceholderColor = TextoPlaceholder,
                focusedLeadingIconColor = TextoSuave,
                unfocusedLeadingIconColor = TextoSuave,
                focusedTrailingIconColor = TextoSuave,
                unfocusedTrailingIconColor = TextoSuave,
                cursorColor = Azul
            )
        )
        if (soloLectura && alPresionar != null && habilitado) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Transparent)
                    .clickable(onClick = alPresionar)
            )
        }
        }
        val textoInferior = error ?: ayuda
        if (textoInferior != null) {
            Text(
                text = textoInferior,
                color = if (error != null) MaterialTheme.colorScheme.error else TextoSuave,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                modifier = Modifier.padding(top = 7.dp, start = 2.dp)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 820, widthDp = 390)
@Composable
private fun LoginPreview() {
    YourPeakTheme {
        LoginContenido(
            estado = LoginUiState(aviso = "Registro exitoso. Tu cuenta est\u00E1 pendiente de aprobaci\u00F3n."),
            alCambiarCorreo = {}, alCambiarContrasena = {}, alEnviar = {}, alIrARegistro = {}
        )
    }
}
