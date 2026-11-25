package com.ramirez.yourpeak.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.YourPeakApp
import com.ramirez.yourpeak.ui.components.EncabezadoTarjeta
import com.ramirez.yourpeak.ui.components.IconoEntrenamiento
import com.ramirez.yourpeak.ui.components.IconoInicio
import com.ramirez.yourpeak.ui.components.IconoMembresia
import com.ramirez.yourpeak.ui.components.IconoNutricion
import com.ramirez.yourpeak.ui.components.IconoProgreso
import com.ramirez.yourpeak.ui.components.IconoPerfil
import com.ramirez.yourpeak.ui.components.NotaPie
import com.ramirez.yourpeak.ui.components.TarjetaAuth
import com.ramirez.yourpeak.ui.theme.YourPeakTheme

private val tipografiaTituloCondensada = FontFamily(
    android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL)
)

private val seccionesEntrenamiento = listOf(SeccionCliente.MI_RUTINA, SeccionCliente.EJERCICIOS, SeccionCliente.DISPONIBILIDAD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    usuario: LoginResponse,
    alCerrarSesion: () -> Unit,
    viewModel: ClienteHomeViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val app = LocalContext.current.applicationContext as? YourPeakApp

    Scaffold(
        topBar = {
            if (estado.seccionSeleccionada != SeccionCliente.PERFIL) TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                title = {
                    Text(
                        text = "YOUR PEAK",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.seleccionarSeccion(SeccionCliente.PERFIL) }) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color(0xFF202020),
                            border = BorderStroke(1.dp, Color(0xFF3A3A3A))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                IconoPerfil(Color.White, Modifier.size(23.dp))
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (estado.seccionSeleccionada != SeccionCliente.PERFIL) {
                NavegacionPrincipal(
                    seccionSeleccionada = estado.seccionSeleccionada,
                    alSeleccionar = viewModel::seleccionarSeccion
                )
            }
        },
        containerColor = Color.Black
    ) { contenidoPadding ->
        Box(Modifier.fillMaxSize().background(Color.Black).padding(contenidoPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (estado.seccionSeleccionada == SeccionCliente.MI_RUTINA || estado.seccionSeleccionada == SeccionCliente.PERFIL) Modifier
                        else Modifier.verticalScroll(rememberScrollState())
                    )
                    .padding(
                        start = 20.dp,
                        top = 24.dp,
                        end = 20.dp,
                        bottom = if (estado.seccionSeleccionada == SeccionCliente.MI_RUTINA) 0.dp else 24.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxWidth().then(
                        if (estado.seccionSeleccionada == SeccionCliente.PERFIL) Modifier.weight(1f) else Modifier
                    )
                ) {
                    val esInicio = estado.seccionSeleccionada == SeccionCliente.INICIO
                    val esTituloGrande = estado.seccionSeleccionada in seccionesEntrenamiento ||
                        estado.seccionSeleccionada == SeccionCliente.NUTRICION ||
                        estado.seccionSeleccionada == SeccionCliente.PROGRESO ||
                        estado.seccionSeleccionada == SeccionCliente.MEMBRESIA || esInicio
                    if (estado.seccionSeleccionada != SeccionCliente.PERFIL) Text(
                        text = buildAnnotatedString {
                            if (esInicio) {
                                append("HOLA, ${usuario.nombre.orEmpty().uppercase()}")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(".") }
                            } else if (estado.seccionSeleccionada in seccionesEntrenamiento) {
                                append("ENTRENAMIENTO")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(".") }
                            } else if (estado.seccionSeleccionada == SeccionCliente.NUTRICION) {
                                append("NUTRICIÓN")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(".") }
                            } else if (estado.seccionSeleccionada == SeccionCliente.PROGRESO) {
                                append("MI PROGRESO")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(".") }
                            } else if (estado.seccionSeleccionada == SeccionCliente.MEMBRESIA) {
                                append("MI MEMBRESÍA")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(".") }
                            } else {
                                append(estado.seccionSeleccionada.titulo.uppercase())
                            }
                        },
                        style = if (esTituloGrande) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.labelLarge,
                        color = if (esTituloGrande) Color.White else MaterialTheme.colorScheme.tertiary,
                        fontFamily = if (esTituloGrande) tipografiaTituloCondensada else FontFamily.SansSerif,
                        fontSize = if (esTituloGrande) 42.sp else 14.sp,
                        lineHeight = if (esTituloGrande) 46.sp else 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = if (esTituloGrande) (-1.4).sp else 0.sp,
                        maxLines = 2
                    )
                    if (estado.seccionSeleccionada != SeccionCliente.PERFIL) Spacer(Modifier.height(if (esInicio) 1.dp else 8.dp))
                    if (!esInicio && estado.seccionSeleccionada != SeccionCliente.PERFIL && estado.seccionSeleccionada !in seccionesEntrenamiento && estado.seccionSeleccionada != SeccionCliente.NUTRICION && estado.seccionSeleccionada != SeccionCliente.PROGRESO && estado.seccionSeleccionada != SeccionCliente.MEMBRESIA) {
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
                    }
                    if ((estado.seccionSeleccionada in seccionesEntrenamiento)) {
                        NavegacionEntrenamiento(
                            seccionSeleccionada = estado.seccionSeleccionada,
                            alSeleccionar = viewModel::seleccionarSeccion
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    val contenidoSeccion: @Composable () -> Unit = {
                        ContenidoSeccion(
                            seccion = estado.seccionSeleccionada,
                            modifier = if (estado.seccionSeleccionada == SeccionCliente.MI_RUTINA || estado.seccionSeleccionada == SeccionCliente.PERFIL) {
                                Modifier.weight(1f)
                            } else Modifier,
                            usuario = usuario,
                            ejercicioRepository = app?.ejercicioRepository,
                            disponibilidadRepository = app?.disponibilidadRepository,
                            rutinaRepository = app?.rutinaRepository,
                            nutricionRepository = app?.nutricionRepository,
                            membresiaClienteRepository = app?.membresiaClienteRepository,
                            progresoRepository = app?.progresoRepository,
                            perfilRepository = app?.perfilRepository,
                            alIrASeccion = viewModel::seleccionarSeccion,
                            alCerrarSesion = alCerrarSesion,
                            alIrADisponibilidad = {
                                viewModel.seleccionarSeccion(SeccionCliente.DISPONIBILIDAD)
                            }
                        )
                    }
                    if (estado.seccionSeleccionada == SeccionCliente.INICIO || estado.seccionSeleccionada in seccionesEntrenamiento || estado.seccionSeleccionada == SeccionCliente.NUTRICION || estado.seccionSeleccionada == SeccionCliente.PROGRESO || estado.seccionSeleccionada == SeccionCliente.MEMBRESIA || estado.seccionSeleccionada == SeccionCliente.PERFIL) {
                        contenidoSeccion()
                    } else {
                        TarjetaAuth { contenidoSeccion() }
                        NotaPie()
                    }
                }
            }
        }
    }
}

/** Bottom navigation for the five main client sections. */
@Composable
private fun NavegacionPrincipal(
    seccionSeleccionada: SeccionCliente,
    alSeleccionar: (SeccionCliente) -> Unit
) {
    val entrenamientoSeleccionado = seccionSeleccionada in seccionesEntrenamiento
    val destinos = listOf(
        SeccionCliente.INICIO to "Inicio",
        SeccionCliente.MI_RUTINA to "Entrenamiento",
        SeccionCliente.NUTRICION to "Nutrici\u00F3n",
        SeccionCliente.PROGRESO to "Progreso",
        SeccionCliente.MEMBRESIA to "Membres\u00EDa"
    )

    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF151515),
            border = BorderStroke(1.dp, Color(0xFF292929)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 5.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinos.forEach { (seccion, etiqueta) ->
                    val seleccionado = if (seccion == SeccionCliente.MI_RUTINA) {
                        entrenamientoSeleccionado
                    } else seccionSeleccionada == seccion
                    val colorContenido = if (seleccionado) Color.White else Color(0xFF9A9A9A)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (seleccionado) Color(0xFF292929) else Color.Transparent)
                            .clickable { alSeleccionar(seccion) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (seccion) {
                            SeccionCliente.INICIO -> IconoInicio(colorContenido)
                            SeccionCliente.MI_RUTINA -> IconoEntrenamiento(colorContenido)
                            SeccionCliente.NUTRICION -> IconoNutricion(colorContenido)
                            SeccionCliente.PROGRESO -> IconoProgreso(colorContenido)
                            SeccionCliente.MEMBRESIA -> IconoMembresia(colorContenido)
                            else -> Unit
                        }
                        Text(
                            text = etiqueta,
                            color = colorContenido,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            maxLines = 1,
                            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
/** Conserva Mi rutina, Ejercicios y Disponibilidad como destinos internos de Entrenamiento. */
@Composable
private fun NavegacionEntrenamiento(
    seccionSeleccionada: SeccionCliente,
    alSeleccionar: (SeccionCliente) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF111111),
        border = BorderStroke(1.dp, Color(0xFF292929))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            seccionesEntrenamiento.forEach { seccion ->
                val seleccionado = seccion == seccionSeleccionada
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (seleccionado) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { alSeleccionar(seccion) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (seccion) {
                            SeccionCliente.MI_RUTINA -> "Mi rutina"
                            SeccionCliente.EJERCICIOS -> "Ejercicios"
                            else -> "Disponibilidad"
                        },
                        color = if (seleccionado) Color.White else Color(0xFFB8BCC6),
                        fontSize = 14.sp,
                        fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
@Composable
private fun ContenidoSeccion(
    seccion: SeccionCliente,
    modifier: Modifier = Modifier,
    usuario: LoginResponse,
    ejercicioRepository: com.ramirez.yourpeak.data.repository.EjercicioRepository?,
    disponibilidadRepository: com.ramirez.yourpeak.data.repository.DisponibilidadRepository?,
    rutinaRepository: com.ramirez.yourpeak.data.repository.RutinaRepository?,
    nutricionRepository: com.ramirez.yourpeak.data.repository.NutricionRepository?,
    membresiaClienteRepository: com.ramirez.yourpeak.data.repository.MembresiaClienteRepository?,
    progresoRepository: com.ramirez.yourpeak.data.repository.ProgresoRepository?,
    perfilRepository: com.ramirez.yourpeak.data.repository.PerfilRepository?,
    alIrASeccion: (SeccionCliente) -> Unit,
    alCerrarSesion: () -> Unit,
    alIrADisponibilidad: () -> Unit
) {
    when (seccion) {
        SeccionCliente.INICIO -> {
            if (rutinaRepository != null && nutricionRepository != null && progresoRepository != null) {
                InicioScreen(
                    idUsuario = usuario.idUsuario,
                    rutinaRepository = rutinaRepository,
                    nutricionRepository = nutricionRepository,
                    progresoRepository = progresoRepository,
                    alIrARutina = { alIrASeccion(SeccionCliente.MI_RUTINA) },
                    alIrANutricion = { alIrASeccion(SeccionCliente.NUTRICION) },
                    alIrAProgreso = { alIrASeccion(SeccionCliente.PROGRESO) }
                )
            } else SeccionProximamente("Inicio", "No se pudieron preparar los resúmenes de tu cuenta.")
        }
        SeccionCliente.MI_RUTINA -> if (
            rutinaRepository != null && ejercicioRepository != null
        ) {
            MiRutinaScreen(
                modifier = modifier,
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
        SeccionCliente.PROGRESO -> progresoRepository?.let {
            ProgresoScreen(usuario.idUsuario, it)
        } ?: SeccionProximamente(
            titulo = "Mi progreso",
            descripcion = "Aquí podrás registrar tu peso y consultar su evolución."
        )
        SeccionCliente.MEMBRESIA -> membresiaClienteRepository?.let {
            MiMembresiaScreen(usuario, it)
        } ?: SeccionProximamente(
            titulo = "Mi membresía",
            descripcion = "Aquí podrás consultar el estado y vigencia de tu membresía."
        )
        SeccionCliente.PERFIL -> perfilRepository?.let {
            PerfilScreen(
                usuario = usuario,
                repository = it,
                alCerrarSesion = alCerrarSesion,
                alVolver = { alIrASeccion(SeccionCliente.INICIO) }
            )
        } ?: run {
            EncabezadoTarjeta(
                etiqueta = "CUENTA DEL CLIENTE",
                titulo = "Mi perfil",
                introduccion = "Consulta los datos de tu cuenta Your Peak."
            )
            Text("Nombre: ${usuario.nombre.orEmpty()} ${usuario.apellido.orEmpty()}".trim())
            Text("Correo: ${usuario.correo.orEmpty()}")
            Text("Estado de cuenta: ${usuario.estado.orEmpty()}")
            TextButton(onClick = alCerrarSesion) { Text("Cerrar sesión") }
        }
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
