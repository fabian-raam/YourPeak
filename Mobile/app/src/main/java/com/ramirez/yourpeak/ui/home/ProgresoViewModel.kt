package com.ramirez.yourpeak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramirez.yourpeak.data.model.RegistroProgresoRequest
import com.ramirez.yourpeak.data.model.RegistroProgresoResponse
import com.ramirez.yourpeak.data.repository.ProgresoRepository
import com.ramirez.yourpeak.data.repository.ResultadoProgreso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

data class ProgresoUiState(
    val registros: List<RegistroProgresoResponse> = emptyList(),
    val pesoIngresado: String = "",
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null,
    val aviso: String? = null
) {
    val pesoActual: BigDecimal? get() = registros.firstOrNull()?.pesoKg
    val variacion: BigDecimal? get() {
        val actual = pesoActual ?: return null
        val inicial = registros.lastOrNull()?.pesoKg ?: return null
        return actual.subtract(inicial).stripTrailingZeros()
    }
}

/** Gestiona la consulta histórica y el registro de nuevas mediciones de peso. */
class ProgresoViewModel(
    private val repository: ProgresoRepository,
    private val idUsuario: Long?
) : ViewModel() {
    private val _estado = MutableStateFlow(ProgresoUiState())
    val estado: StateFlow<ProgresoUiState> = _estado.asStateFlow()

    init { actualizar() }

    fun cambiarPeso(valor: String) = _estado.update {
        it.copy(pesoIngresado = valor.filter { caracter -> caracter.isDigit() || caracter == '.' || caracter == ',' }, error = null)
    }

    fun actualizar() {
        val id = idUsuario
        if (id == null) {
            _estado.update { it.copy(cargando = false, error = "No se pudo identificar la cuenta.") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = repository.consultar(id)) {
                is ResultadoProgreso.Exito -> _estado.update {
                    it.copy(registros = resultado.dato.sortedByDescending { registro -> registro.fechaRegistro.orEmpty() }, cargando = false)
                }
                is ResultadoProgreso.Error -> _estado.update { it.copy(cargando = false, error = resultado.mensaje) }
            }
        }
    }

    fun registrar() {
        val id = idUsuario
        val peso = _estado.value.pesoIngresado.replace(',', '.').toBigDecimalOrNull()
        val pesoNormalizado = peso?.let {
            runCatching { it.setScale(2, RoundingMode.UNNECESSARY) }.getOrNull()
        }
        if (id == null || pesoNormalizado == null || pesoNormalizado < BigDecimal("0.01") || pesoNormalizado > BigDecimal("999.99")) {
            _estado.update { it.copy(error = "Ingresa un peso entre 0.01 y 999.99 kg, con hasta dos decimales.") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null, aviso = null) }
            when (val resultado = repository.registrar(id, RegistroProgresoRequest(pesoNormalizado))) {
                is ResultadoProgreso.Exito -> {
                    _estado.update { it.copy(pesoIngresado = "", guardando = false, aviso = "Peso registrado en tu historial.") }
                    actualizar()
                }
                is ResultadoProgreso.Error -> _estado.update { it.copy(guardando = false, error = resultado.mensaje) }
            }
        }
    }

    fun cerrarMensajes() = _estado.update { it.copy(error = null, aviso = null) }

    companion object {
        fun factory(repository: ProgresoRepository, idUsuario: Long?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ProgresoViewModel::class.java))
                    return ProgresoViewModel(repository, idUsuario) as T
                }
            }
    }
}
