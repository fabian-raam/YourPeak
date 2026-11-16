package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.YourPeakApp
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.components.FondoMarca
import com.ramirez.yourpeak.ui.components.NotaPie
import com.ramirez.yourpeak.ui.components.TarjetaAuth
import com.ramirez.yourpeak.ui.theme.YourPeakTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    usuario: LoginResponse,
    alCerrarSesion: () -> Unit,
    viewModel: ClienteHomeViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val app = LocalContext.current.applicationContext as? YourPeakApp
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuLateralCliente(
                usuario = usuario,
                seccionSeleccionada = estado.seccionSeleccionada,
                alSeleccionar = { seccion ->
                    viewModel.seleccionarSeccion(seccion)
                    scope.launch { drawerState.close() }
                },
                alCerrarSesion = alCerrarSesion
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("YOUR PEAK") },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.semantics { contentDescription = "Abrir menú lateral" }
                        ) {
                            Text("☰", color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { contenidoPadding ->
            FondoMarca(Modifier.fillMaxSize().padding(contenidoPadding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(Modifier.widthIn(max = 560.dp)) {
                        Text(
                            text = estado.seccionSeleccionada.titulo.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Hola, ${usuario.nombre.orEmpty()}${usuario.apellido?.let { " $it" }.orEmpty()}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tu cuenta está activa.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                        )
                        TarjetaAuth {
                            ContenidoSeccion(
                                seccion = estado.seccionSeleccionada,
                                usuario = usuario,
                                ejercicioRepository = app?.ejercicioRepository,
                                disponibilidadRepository = app?.disponibilidadRepository,
                                rutinaRepository = app?.rutinaRepository,
                                nutricionRepository = app?.nutricionRepository,
                                alIrADisponibilidad = {
                                    viewModel.seleccionarSeccion(SeccionCliente.DISPONIBILIDAD)
                                }
                            )
                        }
                        NotaPie()
                    }
                }
            }
        }
    }
}

@Composable
private fun ContenidoSeccion(
    seccion: SeccionCliente,
    usuario: LoginResponse,
    ejercicioRepository: com.ramirez.yourpeak.data.repository.EjercicioRepository?,
    disponibilidadRepository: com.ramirez.yourpeak.data.repository.DisponibilidadRepository?,
    rutinaRepository: com.ramirez.yourpeak.data.repository.RutinaRepository?,
    nutricionRepository: com.ramirez.yourpeak.data.repository.NutricionRepository?,
    alIrADisponibilidad: () -> Unit
) {
    when (seccion) {
        SeccionCliente.INICIO -> {
            EncabezadoTarjeta(
                etiqueta = "INICIO DEL CLIENTE",
                titulo = "Bienvenido a Your Peak",
                introduccion = "Desde aquí podrás acceder a las herramientas de tu cuenta."
            )
            DatoPerfil("Correo", usuario.correo.orEmpty())
            DatoPerfil("Estado de cuenta", "Activo")
        }
        SeccionCliente.MI_RUTINA -> if (
            rutinaRepository != null && ejercicioRepository != null
        ) {
            MiRutinaScreen(
                idUsuario = usuario.idUsuario,
                rutinaRepository = rutinaRepository,
                ejercicioRepository = ejercicioRepository,
                alIrADisponibilidad = alIrADisponibilidad
            )
        } else SeccionProximamente(
            titulo = "Mi rutina",
            descripcion = "Aquí podrás consultar y organizar tus ejercicios por día."
        )
        SeccionCliente.EJERCICIOS -> ejercicioRepository?.let {
            EjerciciosScreen(usuario.idUsuario, it)
        } ?: SeccionProximamente(
            titulo = "Ejercicios",
            descripcion = "Aquí podrás explorar el catálogo activo del gimnasio."
        )
        SeccionCliente.DISPONIBILIDAD -> disponibilidadRepository?.let {
            DisponibilidadScreen(usuario.idUsuario, it)
        } ?: SeccionProximamente(
            titulo = "Disponibilidad",
            descripcion = "Aquí podrás indicar los días y horarios en los que puedes entrenar."
        )
        SeccionCliente.NUTRICION -> nutricionRepository?.let {
            NutricionScreen(usuario.idUsuario, it)
        } ?: SeccionProximamente(
            titulo = "Nutrición",
            descripcion = "Aquí podrás registrar tus alimentos y revisar tus consumos diarios."
        )
        SeccionCliente.PROGRESO -> SeccionProximamente(
            titulo = "Mi progreso",
            descripcion = "Aquí podrás registrar tu peso y consultar su evolución."
        )
        SeccionCliente.MEMBRESIA -> SeccionProximamente(
            titulo = "Mi membresía",
            descripcion = "Aquí podrás consultar el estado y vigencia de tu membresía."
        )
    }
}

@Composable
private fun SeccionProximamente(titulo: String, descripcion: String) {
    EncabezadoTarjeta(
        etiqueta = "SECCIÓN DEL CLIENTE",
        titulo = titulo,
        introduccion = descripcion
    )
    Text(
        text = "Esta sección móvil se conectará en el siguiente paso.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DatoPerfil(etiqueta: String, valor: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun HomePreview() {
    YourPeakTheme {
        HomeScreen(
            usuario = LoginResponse(1, "Ana", "Torres", "ana@tecsup.edu.pe", "CLIENTE", "ACTIVO"),
            alCerrarSesion = {}
        )
    }
}
