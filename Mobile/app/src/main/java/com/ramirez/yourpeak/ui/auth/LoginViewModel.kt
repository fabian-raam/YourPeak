package com.ramirez.yourpeak.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ramirez.yourpeak.YourPeakApp
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.repository.ApiResult
import com.ramirez.yourpeak.data.repository.AuthRepository
import com.ramirez.yourpeak.data.session.SessionManager
import com.ramirez.yourpeak.domain.AuthValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val errores: Map<String, String> = emptyMap(),
    val mensajeError: String? = null,
    val aviso: String? = null,
    val cargando: Boolean = false,
    /** Se llena cuando Spring acepta las credenciales; la pantalla navega al inicio. */
    val usuarioAutenticado: LoginResponse? = null
)

class LoginViewModel(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    /** Muestra el mensaje de Spring y el correo tras un registro exitoso. */
    fun prepararDesdeRegistro(correo: String?, aviso: String?) {
        if (correo.isNullOrBlank() && aviso.isNullOrBlank()) return
        _estado.update { it.copy(correo = correo.orEmpty(), aviso = aviso) }
    }

    fun cambiarCorreo(valor: String) =
        _estado.update { it.copy(correo = valor, errores = it.errores - "correo", mensajeError = null) }

    fun cambiarContrasena(valor: String) =
        _estado.update { it.copy(contrasena = valor, errores = it.errores - "contrasena", mensajeError = null) }

    fun iniciarSesion() {
        val actual = _estado.value
        if (actual.cargando) return

        val errores = AuthValidator.validarLogin(actual.correo, actual.contrasena)
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores, mensajeError = "Revisa los campos indicados.", aviso = null) }
            return
        }

        _estado.update { it.copy(cargando = true, errores = emptyMap(), mensajeError = null, aviso = null) }
        viewModelScope.launch {
            when (val resultado = repository.login(actual.correo, actual.contrasena)) {
                is ApiResult.Exito -> {
                    sessionManager.guardar(resultado.datos)
                    _estado.update {
                        it.copy(cargando = false, contrasena = "", usuarioAutenticado = resultado.datos)
                    }
                }
                is ApiResult.Error -> _estado.update {
                    it.copy(cargando = false, mensajeError = resultado.mensaje, errores = resultado.campos)
                }
            }
        }
    }

    fun navegacionRealizada() = _estado.update { LoginUiState() }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as YourPeakApp
                LoginViewModel(app.authRepository, app.sessionManager)
            }
        }
    }
}
