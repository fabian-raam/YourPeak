package com.ramirez.yourpeak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.model.SolicitudAccesoResponse
import com.ramirez.yourpeak.data.repository.ResultadoSolicitud
import com.ramirez.yourpeak.data.repository.SolicitudAccesoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoCuentaUiState(
    val usuario: LoginResponse,
    val solicitud: SolicitudAccesoResponse? = null,
    val cargando: Boolean = false,
    val enviando: Boolean = false,
    val mensaje: String = "",
    val aviso: String? = null,
    val error: String? = null
) {
    val ultimoRechazo get() = solicitud?.historial?.firstOrNull { it.estado.equals("RECHAZADA", true) }
    val puedeSolicitar get() = solicitud?.puedeSolicitar == true && !enviando
}

/** Mantiene el estado de la solicitud de acceso y avisa cuando la cuenta cambia de estado. */
class EstadoCuentaViewModel(
    private val repository: SolicitudAccesoRepository,
    usuario: LoginResponse,
    private val consultarSolicitud: Boolean
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoCuentaUiState(usuario))
    val estado: StateFlow<EstadoCuentaUiState> = _estado.asStateFlow()

    init { if (consultarSolicitud) cargarSolicitud() }

    fun cambiarMensaje(texto: String) = _estado.update { it.copy(mensaje = texto.take(300), error = null) }

    /** Vuelve a leer la cuenta en Spring; devuelve el usuario si su estado cambió. */
    fun actualizar(alCambiar: (LoginResponse) -> Unit) {
        val id = _estado.value.usuario.idUsuario ?: return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, aviso = null) }
            when (val resultado = repository.consultarUsuario(id)) {
                is ResultadoSolicitud.Exito -> {
                    val anterior = _estado.value.usuario
                    _estado.update { it.copy(usuario = resultado.datos, cargando = false) }
                    if (!resultado.datos.estado.equals(anterior.estado, true) ||
                        resultado.datos.membresiaVigente != anterior.membresiaVigente) {
                        alCambiar(resultado.datos)
                        return@launch
                    }
                    _estado.update {
                        it.copy(aviso = if (anterior.estado.equals("PENDIENTE", true)) "Tu solicitud sigue en revisión." else "Sin cambios por ahora.")
                    }
                    if (consultarSolicitud) cargarSolicitud()
                }
                is ResultadoSolicitud.Error -> _estado.update { it.copy(cargando = false, error = resultado.mensaje) }
            }
        }
    }

    fun volverASolicitar(alCambiar: (LoginResponse) -> Unit) {
        val actual = _estado.value
        val id = actual.usuario.idUsuario ?: return
        if (!actual.puedeSolicitar) return
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, error = null, aviso = null) }
            when (val resultado = repository.volverASolicitar(id, actual.mensaje)) {
                is ResultadoSolicitud.Exito -> {
                    _estado.update { it.copy(enviando = false, solicitud = resultado.datos, mensaje = "") }
                    alCambiar(actual.usuario.copy(estado = resultado.datos.estado ?: "PENDIENTE"))
                }
                is ResultadoSolicitud.Error -> {
                    _estado.update { it.copy(enviando = false, error = resultado.mensaje) }
                    cargarSolicitud()
                }
            }
        }
    }

    private fun cargarSolicitud() {
        val id = _estado.value.usuario.idUsuario ?: return
        viewModelScope.launch {
            when (val resultado = repository.consultar(id)) {
                is ResultadoSolicitud.Exito -> _estado.update { it.copy(solicitud = resultado.datos) }
                is ResultadoSolicitud.Error -> _estado.update { it.copy(error = it.error ?: resultado.mensaje) }
            }
        }
    }

    companion object {
        fun factory(repository: SolicitudAccesoRepository, usuario: LoginResponse, consultarSolicitud: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(EstadoCuentaViewModel::class.java))
                    return EstadoCuentaViewModel(repository, usuario, consultarSolicitud) as T
                }
            }
    }
}
