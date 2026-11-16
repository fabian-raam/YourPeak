package com.ramirez.yourpeak.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Secciones que la app móvil irá completando para el cliente. */
enum class SeccionCliente(val titulo: String) {
    INICIO("Inicio"),
    MI_RUTINA("Mi rutina"),
    EJERCICIOS("Ejercicios"),
    DISPONIBILIDAD("Disponibilidad"),
    NUTRICION("Nutrición"),
    PROGRESO("Mi progreso"),
    MEMBRESIA("Mi membresía")
}

data class ClienteHomeUiState(
    val seccionSeleccionada: SeccionCliente = SeccionCliente.INICIO
)

/** Mantiene la selección de sección fuera de la pantalla Compose. */
class ClienteHomeViewModel : ViewModel() {

    private val _estado = MutableStateFlow(ClienteHomeUiState())
    val estado: StateFlow<ClienteHomeUiState> = _estado.asStateFlow()

    fun seleccionarSeccion(seccion: SeccionCliente) {
        _estado.update { it.copy(seccionSeleccionada = seccion) }
    }
}
