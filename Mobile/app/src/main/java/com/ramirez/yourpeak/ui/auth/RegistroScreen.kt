package com.ramirez.yourpeak.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.R
import com.ramirez.yourpeak.domain.Fechas
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
fun RegistroScreen(
    alRegistrarse: (correo: String, mensaje: String) -> Unit,
    alIrALogin: () -> Unit,
    viewModel: RegistroViewModel = viewModel(factory = RegistroViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.registroExitoso) {
        estado.registroExitoso?.let { mensaje ->
            alRegistrarse(estado.correo.trim().lowercase(), mensaje)
            viewModel.navegacionRealizada()
        }
    }

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
        )
    )
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
private fun RegistroContenido(estado: RegistroUiState, acciones: RegistroAcciones) {
    val foco = LocalFocusManager.current
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }
    val habilitado = !estado.cargando

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
                        titulo = "Crea tu cuenta",
                        introduccion = "Comienza tu camino con Your Peak."
                    )
                    Aviso(estado.mensajeError, TipoAviso.Error)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CampoTexto(
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
                        CampoTexto(
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
                    CampoTexto(
                        valor = if (estado.fechaNacimiento.isBlank()) "" else Fechas.paraMostrar(estado.fechaNacimiento),
                        alCambiar = {},
                        etiqueta = "Fecha de nacimiento",
                        icono = R.drawable.ic_calendar,
                        placeholder = "dd/mm/aaaa",
                        error = estado.errores["fechaNacimiento"],
                        habilitado = habilitado,
                        soloLectura = true,
                        alPresionar = { foco.clearFocus(); mostrarCalendario = true }
                    )
                    CampoTexto(
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
                    CampoTexto(
                        valor = estado.contrasena,
                        alCambiar = acciones.contrasena,
                        etiqueta = "Contraseña",
                        icono = R.drawable.ic_lock,
                        placeholder = "Ingresa tu contraseña",
                        ayuda = "Entre 8 y 128 caracteres. Puedes usar una frase.",
                        error = estado.errores["contrasena"],
                        esContrasena = true,
                        habilitado = habilitado,
                        longitudMaxima = 128
                    )
                    CampoTexto(
                        valor = estado.confirmacion,
                        alCambiar = acciones.confirmacion,
                        etiqueta = "Confirmar contraseña",
                        icono = R.drawable.ic_lock,
                        placeholder = "Repite tu contraseña",
                        error = estado.errores["confirmacion"],
                        esContrasena = true,
                        habilitado = habilitado,
                        longitudMaxima = 128,
                        ultimo = true,
                        accionesTeclado = KeyboardActions(onDone = { foco.clearFocus(); acciones.enviar() })
                    )

                    Spacer(Modifier.height(8.dp))
                    BotonPrincipal("Crear cuenta", acciones.enviar, cargando = estado.cargando)
                    EnlaceCambio(
                        pregunta = "¿Ya tienes una cuenta?",
                        accion = "Inicia sesión",
                        alPresionar = acciones.irALogin,
                        habilitado = habilitado
                    )
                }
                NotaPie()
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
private fun SelectorFecha(fechaActual: String, alConfirmar: (String) -> Unit, alCancelar: () -> Unit) {
    val inicioDeHoy = remember { Fechas.inicioDeHoyUtcMillis() }
    val estadoCalendario = rememberDatePickerState(
        initialSelectedDateMillis = Fechas.millisUtcDesdeIso(fechaActual),
        selectableDates = object : SelectableDates {
            // Igual que @Past en Spring: solo fechas anteriores a hoy.
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
    YourPeakTheme(darkTheme = false) {
        RegistroContenido(
            estado = RegistroUiState(errores = mapOf("correo" to "Usa tu correo institucional @tecsup.edu.pe.")),
            acciones = RegistroAcciones({}, {}, {}, {}, {}, {}, {}, {})
        )
    }
}
