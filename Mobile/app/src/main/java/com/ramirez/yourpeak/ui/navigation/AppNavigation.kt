package com.ramirez.yourpeak.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ramirez.yourpeak.YourPeakApp
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.session.SessionManager
import com.ramirez.yourpeak.domain.DestinoUsuario
import com.ramirez.yourpeak.domain.determinarDestinoUsuario
import com.ramirez.yourpeak.ui.auth.LoginScreen
import com.ramirez.yourpeak.ui.auth.RegistroScreen
import com.ramirez.yourpeak.ui.home.EstadoAccesoScreen
import com.ramirez.yourpeak.ui.home.HomeScreen
import com.ramirez.yourpeak.ui.splash.SplashScreen

private object Rutas {
    const val SPLASH = "splash"
    const val LOGIN = "login?correo={correo}&aviso={aviso}"
    const val REGISTRO = "registro"
    const val INICIO_CLIENTE = "cliente/inicio"
    const val PENDIENTE = "cuenta/pendiente"
    const val RECHAZADO = "cuenta/rechazada"
    const val INACTIVO = "cuenta/inactiva"
    const val ADMINISTRADOR = "administrador/informacion"
    const val SIN_ACCESO = "cuenta/sin-acceso"

    fun login(correo: String? = null, aviso: String? = null): String =
        "login?correo=${Uri.encode(correo.orEmpty())}&aviso=${Uri.encode(aviso.orEmpty())}"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as YourPeakApp
    val sesion = app.sessionManager

    fun navegarSinVolver(ruta: String) {
        navController.navigate(ruta) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navegarSegunCuenta(usuario: LoginResponse) {
        navegarSinVolver(rutaPara(determinarDestinoUsuario(usuario.rol, usuario.estado)))
    }

    // La cuenta cambió de estado (aprobada, rechazada o reenviada): se guarda y se lleva a su pantalla.
    fun cambiarEstado(usuario: LoginResponse) {
        sesion.guardar(usuario)
        navegarSegunCuenta(usuario)
    }

    fun cerrarSesion() {
        sesion.cerrarSesion()
        navegarSinVolver(Rutas.SPLASH)
    }

    NavHost(navController = navController, startDestination = Rutas.SPLASH) {
        composable(Rutas.SPLASH) {
            val haySesion = remember { sesion.usuarioActual() != null }
            SplashScreen(
                haySesion = haySesion,
                alContinuarConSesion = {
                    val usuario = sesion.usuarioActual()
                    if (usuario == null) navegarSinVolver(Rutas.login())
                    else navegarSegunCuenta(usuario)
                },
                alIniciarSesion = { navController.navigate(Rutas.login()) },
                alRegistrarse = { navController.navigate(Rutas.REGISTRO) }
            )
        }

        composable(
            route = Rutas.LOGIN,
            arguments = listOf(
                navArgument("correo") { type = NavType.StringType; defaultValue = "" },
                navArgument("aviso") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entrada ->
            LoginScreen(
                correoInicial = entrada.arguments?.getString("correo"),
                avisoInicial = entrada.arguments?.getString("aviso"),
                alIniciarSesion = { usuario -> navegarSegunCuenta(usuario) },
                alIrARegistro = {
                    navController.navigate(Rutas.REGISTRO) { popUpTo(Rutas.SPLASH) }
                }
            )
        }

        composable(Rutas.REGISTRO) {
            RegistroScreen(
                alIniciarSesionConGoogle = ::navegarSegunCuenta,
                alRegistrarse = { correo, mensaje ->
                    navController.navigate(Rutas.login(correo, mensaje)) {
                        popUpTo(Rutas.SPLASH)
                    }
                },
                alIrALogin = {
                    navController.navigate(Rutas.login()) { popUpTo(Rutas.SPLASH) }
                }
            )
        }

        composable(Rutas.INICIO_CLIENTE) {
            DestinoProtegido(
                sesion = sesion,
                navController = navController,
                esperado = DestinoUsuario.INICIO_CLIENTE
            ) { usuario ->
                HomeScreen(usuario = usuario, alCerrarSesion = { cerrarSesion() })
            }
        }

        composable(Rutas.PENDIENTE) {
            PantallaEstadoCuenta(sesion, navController, DestinoUsuario.CUENTA_PENDIENTE, app, ::cambiarEstado) { cerrarSesion() }
        }
        composable(Rutas.RECHAZADO) {
            PantallaEstadoCuenta(sesion, navController, DestinoUsuario.CUENTA_RECHAZADA, app, ::cambiarEstado) { cerrarSesion() }
        }
        composable(Rutas.INACTIVO) {
            PantallaEstadoCuenta(sesion, navController, DestinoUsuario.CUENTA_INACTIVA, app, ::cambiarEstado) { cerrarSesion() }
        }
        composable(Rutas.ADMINISTRADOR) {
            PantallaEstadoCuenta(sesion, navController, DestinoUsuario.INFORMACION_ADMINISTRADOR, app, ::cambiarEstado) { cerrarSesion() }
        }
        composable(Rutas.SIN_ACCESO) {
            PantallaEstadoCuenta(sesion, navController, DestinoUsuario.ACCESO_NO_RECONOCIDO, app, ::cambiarEstado) { cerrarSesion() }
        }
    }
}

@Composable
private fun PantallaEstadoCuenta(
    sesion: SessionManager,
    navController: NavHostController,
    esperado: DestinoUsuario,
    app: YourPeakApp,
    alCambiarEstado: (LoginResponse) -> Unit,
    alCerrarSesion: () -> Unit
) {
    DestinoProtegido(sesion, navController, esperado) { usuario ->
        EstadoAccesoScreen(esperado, usuario, app.solicitudAccesoRepository, alCambiarEstado, alCerrarSesion)
    }
}

/** Revalida que una ruta guardada coincida con el rol y estado de la sesión local. */
@Composable
private fun DestinoProtegido(
    sesion: SessionManager,
    navController: NavHostController,
    esperado: DestinoUsuario,
    contenido: @Composable (LoginResponse) -> Unit
) {
    val usuario = remember { sesion.usuarioActual() }
    val destinoActual = usuario?.let { determinarDestinoUsuario(it.rol, it.estado) }

    if (usuario == null || destinoActual != esperado) {
        LaunchedEffect(usuario, destinoActual) {
            val siguiente = destinoActual?.let(::rutaPara) ?: Rutas.SPLASH
            navController.navigate(siguiente) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    } else {
        contenido(usuario)
    }
}

private fun rutaPara(destino: DestinoUsuario): String = when (destino) {
    DestinoUsuario.INICIO_CLIENTE -> Rutas.INICIO_CLIENTE
    DestinoUsuario.CUENTA_PENDIENTE -> Rutas.PENDIENTE
    DestinoUsuario.CUENTA_RECHAZADA -> Rutas.RECHAZADO
    DestinoUsuario.CUENTA_INACTIVA -> Rutas.INACTIVO
    DestinoUsuario.INFORMACION_ADMINISTRADOR -> Rutas.ADMINISTRADOR
    DestinoUsuario.ACCESO_NO_RECONOCIDO -> Rutas.SIN_ACCESO
}
