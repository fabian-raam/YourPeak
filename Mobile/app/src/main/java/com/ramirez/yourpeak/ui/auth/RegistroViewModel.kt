package com.ramirez.yourpeak.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ramirez.yourpeak.YourPeakApp
import com.ramirez.yourpeak.data.repository.ApiResult
import com.ramirez.yourpeak.data.repository.AuthRepository
import com.ramirez.yourpeak.domain.AuthValidator
import com.ramirez.yourpeak.domain.Fechas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistroUiState(
    val nombre: String = "",
    val apellido: String = "",
    /** yyyy-MM-dd, tal como lo espera Spring. */
    val fechaNacimiento: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val confirmacion: String = "",
    val errores: Map<String, String> = emptyMap(),
    val mensajeError: String? = null,
    val cargando: Boolean = false,
    /** Mensaje de Spring tras crear la cuenta; la pantalla vuelve al login. */
    val registroExitoso: String? = null
)

class RegistroViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado: StateFlow<RegistroUiState> = _estado.asStateFlow()

    fun cambiarNombre(valor: String) = actualizar("nombre") { it.copy(nombre = valor) }
    fun cambiarApellido(valor: String) = actualizar("apellido") { it.copy(apellido = valor) }
    fun cambiarFecha(valor: String) = actualizar("fechaNacimiento") { it.copy(fechaNacimiento = valor) }
    fun cambiarCorreo(valor: String) = actualizar("correo") { it.copy(correo = valor) }
    fun cambiarContrasena(valor: String) = actualizar("contrasena") { it.copy(contrasena = valor) }
    fun cambiarConfirmacion(valor: String) = actualizar("confirmacion") { it.copy(confirmacion = valor) }

    private fun actualizar(campo: String, cambio: (RegistroUiState) -> RegistroUiState) =
        _estado.update { cambio(it).copy(errores = it.errores - campo, mensajeError = null) }

    fun registrar() {
        val actual = _estado.value
        if (actual.cargando) return

        val errores = AuthValidator.validarRegistro(
            nombre = actual.nombre,
            apellido = actual.apellido,
            fechaNacimiento = actual.fechaNacimiento,
            correo = actual.correo,
            contrasena = actual.contrasena,
            confirmacion = actual.confirmacion,
            hoy = Fechas.hoyIso()
        )
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores, mensajeError = "Revisa los campos indicados.") }
            return
        }

        _estado.update { it.copy(cargando = true, errores = emptyMap(), mensajeError = null) }
        viewModelScope.launch {
            val resultado = repository.registrar(
                nombre = actual.nombre,
                apellido = actual.apellido,
                fechaNacimiento = actual.fechaNacimiento,
                correo = actual.correo,
                contrasena = actual.contrasena
            )
            when (resultado) {
                is ApiResult.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        registroExitoso = resultado.datos.mensaje
                            ?: "Registro exitoso. Tu cuenta está pendiente de aprobación."
                    )
                }
                is ApiResult.Error -> _estado.update {
                    it.copy(cargando = false, mensajeError = resultado.mensaje, errores = resultado.campos)
                }
            }
        }
    }

    fun navegacionRealizada() = _estado.update { RegistroUiState() }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as YourPeakApp
                RegistroViewModel(app.authRepository)
            }
        }
    }
}
