package com.ramirez.yourpeak.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.R
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.ui.components.Aviso
import com.ramirez.yourpeak.ui.components.BotonPrincipal
import com.ramirez.yourpeak.ui.components.CampoTexto
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.components.EnlaceCambio
import com.ramirez.yourpeak.ui.components.FondoMarca
import com.ramirez.yourpeak.ui.components.MarcaYourPeak
import com.ramirez.yourpeak.ui.components.NotaPie
import com.ramirez.yourpeak.ui.components.TarjetaAuth
import com.ramirez.yourpeak.ui.components.TipoAviso
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

    FondoMarca(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 520.dp)) {
                MarcaYourPeak(Modifier.padding(start = 6.dp, top = 8.dp))
                Spacer(Modifier.height(28.dp))

                TarjetaAuth {
                    EncabezadoTarjeta(
                        etiqueta = "COMUNIDAD TECSUP",
                        titulo = "Iniciar sesión",
                        introduccion = "Bienvenido de nuevo a Your Peak."
                    )
                    Aviso(estado.aviso, TipoAviso.Exito)
                    Aviso(estado.mensajeError, TipoAviso.Error)

                    CampoTexto(
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
                    CampoTexto(
                        valor = estado.contrasena,
                        alCambiar = alCambiarContrasena,
                        etiqueta = "Contraseña",
                        icono = R.drawable.ic_lock,
                        placeholder = "Ingresa tu contraseña",
                        error = estado.errores["contrasena"],
                        esContrasena = true,
                        habilitado = !estado.cargando,
                        longitudMaxima = 128,
                        ultimo = true,
                        accionesTeclado = KeyboardActions(onDone = { foco.clearFocus(); alEnviar() })
                    )

                    Spacer(Modifier.height(8.dp))
                    BotonPrincipal("Iniciar sesión", alEnviar, cargando = estado.cargando)
                    EnlaceCambio(
                        pregunta = "¿No tienes una cuenta?",
                        accion = "Regístrate",
                        alPresionar = alIrARegistro,
                        habilitado = !estado.cargando
                    )
                }
                NotaPie()
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 820)
@Composable
private fun LoginPreview() {
    YourPeakTheme(darkTheme = false) {
        LoginContenido(
            estado = LoginUiState(aviso = "Registro exitoso. Tu cuenta está pendiente de aprobación."),
            alCambiarCorreo = {}, alCambiarContrasena = {}, alEnviar = {}, alIrARegistro = {}
        )
    }
}
