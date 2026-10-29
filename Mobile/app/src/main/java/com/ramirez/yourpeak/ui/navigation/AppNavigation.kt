package com.ramirez.yourpeak.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ramirez.yourpeak.YourPeakApp
import com.ramirez.yourpeak.ui.auth.LoginScreen
import com.ramirez.yourpeak.ui.auth.RegistroScreen
import com.ramirez.yourpeak.ui.home.HomeScreen
import com.ramirez.yourpeak.ui.splash.SplashScreen

private object Rutas {
    const val SPLASH = "splash"
    const val LOGIN = "login?correo={correo}&aviso={aviso}"
    const val REGISTRO = "registro"
    const val INICIO = "inicio"

    fun login(correo: String? = null, aviso: String? = null): String =
        "login?correo=${Uri.encode(correo.orEmpty())}&aviso=${Uri.encode(aviso.orEmpty())}"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val sesion = (LocalContext.current.applicationContext as YourPeakApp).sessionManager

    NavHost(navController = navController, startDestination = Rutas.SPLASH) {

        composable(Rutas.SPLASH) {
            val haySesion = remember { sesion.usuarioActual() != null }
            SplashScreen(
                haySesion = haySesion,
                alContinuarConSesion = {
                    navController.navigate(Rutas.INICIO) {
                        popUpTo(Rutas.SPLASH) { inclusive = true }
                    }
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
                alIniciarSesion = {
                    navController.navigate(Rutas.INICIO) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                alIrARegistro = {
                    navController.navigate(Rutas.REGISTRO) {
                        popUpTo(Rutas.SPLASH)
                    }
                }
            )
        }

        composable(Rutas.REGISTRO) {
            RegistroScreen(
                alRegistrarse = { correo, mensaje ->
                    navController.navigate(Rutas.login(correo, mensaje)) {
                        popUpTo(Rutas.SPLASH)
                    }
                },
                alIrALogin = {
                    navController.navigate(Rutas.login()) {
                        popUpTo(Rutas.SPLASH)
                    }
                }
            )
        }

        composable(Rutas.INICIO) {
            val usuario = remember { sesion.usuarioActual() }
            if (usuario == null) {
                // Sesión borrada o dañada: volver a la bienvenida.
                LaunchedEffect(Unit) {
                    navController.navigate(Rutas.SPLASH) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            } else {
                HomeScreen(
                    usuario = usuario,
                    alCerrarSesion = {
                        sesion.cerrarSesion()
                        navController.navigate(Rutas.SPLASH) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
