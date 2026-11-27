package com.ramirez.yourpeak.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.R
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.domain.Fechas
import com.ramirez.yourpeak.ui.components.Aviso
import com.ramirez.yourpeak.ui.components.TipoAviso
import com.ramirez.yourpeak.ui.theme.Azul
import com.ramirez.yourpeak.ui.theme.Texto
import com.ramirez.yourpeak.ui.theme.TextoSuave
import com.ramirez.yourpeak.ui.theme.YourPeakTheme

@Composable
fun RegistroScreen(
    alRegistrarse: (correo: String, mensaje: String) -> Unit,
    alIrALogin: () -> Unit,
    alIniciarSesionConGoogle: (LoginResponse) -> Unit,
    viewModel: RegistroViewModel = viewModel(factory = RegistroViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.registroExitoso) {
        estado.registroExitoso?.let { mensaje ->
            alRegistrarse(estado.correo.trim().lowercase(), mensaje)
            viewModel.navegacionRealizada()
        }
    }

    GoogleAccess("Registrarse con Google", estado.cargando, alIniciarSesionConGoogle) { googleBusy, googleButton ->
        RegistroContenido(
            estado = estado,
            acciones = RegistroAcciones(
                nombre = viewModel::cambiarNombre,
                apellido = viewModel::cambiarApellido,
                fecha = viewModel::cambiarFecha,
                correo = viewModel::cambiarCorreo,
                contrasena = viewModel::cambiarContrasena,
                confirmacion = viewModel::cambiarConfirmacion,
                enviar = viewModel::registrar,
                irALogin = alIrALogin
            ),
            googleBusy = googleBusy,
            googleButton = googleButton
        )
    }
}

private class RegistroAcciones(
    val nombre: (String) -> Unit,
    val apellido: (String) -> Unit,
    val fecha: (String) -> Unit,
    val correo: (String) -> Unit,
    val contrasena: (String) -> Unit,
    val confirmacion: (String) -> Unit,
    val enviar: () -> Unit,
    val irALogin: () -> Unit
)

@Composable
private fun RegistroContenido(
    estado: RegistroUiState,
    acciones: RegistroAcciones,
    googleBusy: Boolean = false,
    googleButton: @Composable () -> Unit = { GoogleButton("Registrarse con Google") {} }
) {
    val foco = LocalFocusManager.current
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }
    var mostrarContrasena by rememberSaveable { mutableStateOf(false) }
    var mostrarConfirmacion by rememberSaveable { mutableStateOf(false) }
    val habilitado = !estado.cargando && !googleBusy

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val altura = maxHeight
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
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(altura * 0.075f))
            Text(
                text = "CREA TU CUENTA.",
                color = Texto,
                fontWeight = FontWeight.Black,
                fontSize = 42.sp,
                lineHeight = 46.sp,
                letterSpacing = (-1.4).sp,
                maxLines = 1
            )
            Text(
                text = "Comienza tu camino con Your Peak.",
                color = TextoSuave,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
            )
            Spacer(Modifier.height(12.dp))
            Aviso(estado.mensajeError, TipoAviso.Error)
            googleButton()
            GoogleDivider()

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoAutenticacion(
                    valor = estado.nombre,
                    alCambiar = acciones.nombre,
                    etiqueta = "Nombre",
                    icono = null,
                    placeholder = "Tu nombre",
                    error = estado.errores["nombre"],
                    habilitado = habilitado,
                    longitudMaxima = 100,
                    modifier = Modifier.weight(1f)
                )
                CampoAutenticacion(
                    valor = estado.apellido,
                    alCambiar = acciones.apellido,
                    etiqueta = "Apellido",
                    icono = null,
                    placeholder = "Tu apellido",
                    error = estado.errores["apellido"],
                    habilitado = habilitado,
                    longitudMaxima = 100,
                    modifier = Modifier.weight(1f)
                )
            }
            CampoAutenticacion(
                valor = if (estado.fechaNacimiento.isBlank()) "" else Fechas.paraMostrar(estado.fechaNacimiento),
                alCambiar = {},
                etiqueta = "Fecha de nacimiento",
                icono = R.drawable.ic_calendar,
                placeholder = "dd/mm/aaaa",
                error = estado.errores["fechaNacimiento"],
                habilitado = habilitado,
                soloLectura = true,
                longitudMaxima = 10,
                alPresionar = { foco.clearFocus(); mostrarCalendario = true }
            )
            CampoAutenticacion(
                valor = estado.correo,
                alCambiar = acciones.correo,
                etiqueta = "Correo institucional",
                icono = R.drawable.ic_mail,
                placeholder = "nombre@tecsup.edu.pe",
                ayuda = "Acceso exclusivo con tu correo @tecsup.edu.pe.",
                error = estado.errores["correo"],
                tipoTeclado = KeyboardType.Email,
                habilitado = habilitado,
                longitudMaxima = 254
            )
            CampoAutenticacion(
                valor = estado.contrasena,
                alCambiar = acciones.contrasena,
                etiqueta = "Contrase\u00F1a",
                icono = R.drawable.ic_lock,
                placeholder = "Ingresa tu contrase\u00F1a",
                ayuda = "Entre 8 y 128 caracteres. Puedes usar una frase.",
                error = estado.errores["contrasena"],
                esContrasena = true,
                contrasenaVisible = mostrarContrasena,
                alCambiarVisibilidad = { mostrarContrasena = !mostrarContrasena },
                habilitado = habilitado,
                longitudMaxima = 128,
                ultimo = false
            )
            CampoAutenticacion(
                valor = estado.confirmacion,
                alCambiar = acciones.confirmacion,
                etiqueta = "Confirmar contrase\u00F1a",
                icono = R.drawable.ic_lock,
                placeholder = "Repite tu contrase\u00F1a",
                error = estado.errores["confirmacion"],
                esContrasena = true,
                contrasenaVisible = mostrarConfirmacion,
                alCambiarVisibilidad = { mostrarConfirmacion = !mostrarConfirmacion },
                habilitado = habilitado,
                longitudMaxima = 128,
                ultimo = true,
                alEnviar = { foco.clearFocus(); acciones.enviar() }
            )

            Spacer(Modifier.height(2.dp))
            BotonAutenticacion(
                texto = "CREAR CUENTA",
                textoEnProceso = "CREANDO CUENTA...",
                alPresionar = { foco.clearFocus(); acciones.enviar() },
                cargando = estado.cargando,
                habilitado = !googleBusy
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u00BFYa tienes una cuenta?",
                    color = TextoSuave,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
                )
                TextButton(onClick = acciones.irALogin, enabled = habilitado) {
                    Text(
                        text = "Inicia sesi\u00F3n",
                        color = Azul,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    if (mostrarCalendario) {
        SelectorFecha(
            fechaActual = estado.fechaNacimiento,
            alConfirmar = { acciones.fecha(it); mostrarCalendario = false },
            alCancelar = { mostrarCalendario = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectorFecha(fechaActual: String, alConfirmar: (String) -> Unit, alCancelar: () -> Unit) {
    val inicioDeHoy = remember { Fechas.inicioDeHoyUtcMillis() }
    val estadoCalendario = rememberDatePickerState(
        initialSelectedDateMillis = Fechas.millisUtcDesdeIso(fechaActual),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis < inicioDeHoy
        }
    )
    val colores = MaterialTheme.colorScheme

    DatePickerDialog(
        onDismissRequest = alCancelar,
        confirmButton = {
            TextButton(
                onClick = { estadoCalendario.selectedDateMillis?.let { alConfirmar(Fechas.isoDesdeMillisUtc(it)) } },
                enabled = estadoCalendario.selectedDateMillis != null
            ) { Text("Aceptar", color = colores.tertiary) }
        },
        dismissButton = {
            TextButton(onClick = alCancelar) { Text("Cancelar", color = colores.onSurfaceVariant) }
        },
        colors = DatePickerDefaults.colors(containerColor = colores.surface)
    ) {
        DatePicker(
            state = estadoCalendario,
            colors = DatePickerDefaults.colors(
                containerColor = colores.surface,
                selectedDayContainerColor = colores.primary,
                selectedDayContentColor = colores.onPrimary,
                todayDateBorderColor = colores.tertiary,
                todayContentColor = colores.tertiary,
                selectedYearContainerColor = colores.primary,
                selectedYearContentColor = colores.onPrimary
            )
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun RegistroPreview() {
    YourPeakTheme {
        RegistroContenido(
            estado = RegistroUiState(errores = mapOf("correo" to "Usa tu correo institucional @tecsup.edu.pe.")),
            acciones = RegistroAcciones({}, {}, {}, {}, {}, {}, {}, {})
        )
    }
}
