package com.ramirez.yourpeak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramirez.yourpeak.data.model.EjercicioResponse
import com.ramirez.yourpeak.data.repository.EjercicioRepository
import com.ramirez.yourpeak.data.repository.ResultadoEjercicios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EjerciciosUiState(
    val ejercicios: List<EjercicioResponse> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val busqueda: String = "",
    val grupoSeleccionado: String? = null,
    val nivelSeleccionado: String? = null,
    val ejercicioSeleccionado: Int? = null
) {
    val grupos: List<String>
        get() = ejercicios.mapNotNull { it.grupoMuscular }.distinct().sorted()

    val niveles: List<String>
        get() = ejercicios.mapNotNull { it.nivelExperiencia }.distinct().sorted()

    val ejerciciosFiltrados: List<EjercicioResponse>
        get() = ejercicios.filter { ejercicio ->
            val coincideTexto = listOfNotNull(
                ejercicio.nombre, ejercicio.grupoMuscular, ejercicio.equipamiento
            ).any { it.contains(busqueda.trim(), ignoreCase = true) }
            coincideTexto && (grupoSeleccionado == null || ejercicio.grupoMuscular == grupoSeleccionado) &&
                (nivelSeleccionado == null || ejercicio.nivelExperiencia == nivelSeleccionado)
        }
}

class EjerciciosViewModel(
    private val repository: EjercicioRepository,
    private val idUsuario: Long?
) : ViewModel() {
    private val _estado = MutableStateFlow(EjerciciosUiState(cargando = true))
    val estado: StateFlow<EjerciciosUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    private fun cargar() {
        val id = idUsuario
        if (id == null) {
            _estado.update { it.copy(cargando = false, error = "No se identificó la cuenta.") }
            return
        }
        viewModelScope.launch {
            when (val resultado = repository.listarDisponibles(id)) {
                is ResultadoEjercicios.Exito -> _estado.update {
                    it.copy(ejercicios = resultado.ejercicios, cargando = false, error = null)
                }
                is ResultadoEjercicios.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun reintentar() {
        _estado.update { it.copy(cargando = true, error = null) }
        cargar()
    }

    fun cambiarBusqueda(valor: String) = _estado.update { it.copy(busqueda = valor) }
    fun cambiarGrupo(valor: String?) = _estado.update { it.copy(grupoSeleccionado = valor) }
    fun cambiarNivel(valor: String?) = _estado.update { it.copy(nivelSeleccionado = valor) }
    fun seleccionarEjercicio(id: Int?) = _estado.update { it.copy(ejercicioSeleccionado = id) }

    companion object {
        fun factory(repository: EjercicioRepository, idUsuario: Long?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(EjerciciosViewModel::class.java))
                    return EjerciciosViewModel(repository, idUsuario) as T
                }
            }
    }
}
