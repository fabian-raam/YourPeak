package com.ramirez.yourpeak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.repository.MembresiaClienteRepository
import com.ramirez.yourpeak.data.repository.ResultadoMembresiaCliente
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MiMembresiaUiState(
    val estadoCuenta: String,
    val membresiaVigente: Boolean?,
    val cargando: Boolean = true,
    val error: String? = null
) {
    val tituloEstadoMembresia: String
        get() = when (membresiaVigente) {
            true -> "Vigente"
            false -> "No vigente"
            null -> "Sin información"
        }

    val descripcion: String
        get() = when (membresiaVigente) {
            true -> "Tu membresía está vigente y tu cuenta puede usar las funciones del gimnasio."
            false -> "Tu membresía no está vigente. Comunícate con la administración para renovarla."
            null -> "No recibimos el estado de tu membresía. Vuelve a iniciar sesión para actualizarlo."
        }
}

class MiMembresiaViewModel(
    private val repository: MembresiaClienteRepository,
    private val idUsuario: Long?,
    usuario: LoginResponse
) : ViewModel() {
    private val _estado = MutableStateFlow(MiMembresiaUiState(
        estadoCuenta = usuario.estado.orEmpty(),
        membresiaVigente = usuario.membresiaVigente
    ))
    val estado: StateFlow<MiMembresiaUiState> = _estado.asStateFlow()

    init { actualizar() }

    fun actualizar() {
        val id = idUsuario
        if (id == null) {
            _estado.update { it.copy(cargando = false, error = "No se pudo identificar la cuenta.") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = repository.consultar(id)) {
                is ResultadoMembresiaCliente.Exito -> _estado.update {
                    it.copy(
                        estadoCuenta = resultado.usuario.estado.orEmpty(),
                        membresiaVigente = resultado.usuario.membresiaVigente,
                        cargando = false,
                        error = null
                    )
                }
                is ResultadoMembresiaCliente.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    companion object {
        fun factory(
            repository: MembresiaClienteRepository,
            idUsuario: Long?,
            usuario: LoginResponse
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(MiMembresiaViewModel::class.java))
                return MiMembresiaViewModel(repository, idUsuario, usuario) as T
            }
        }
    }
}
