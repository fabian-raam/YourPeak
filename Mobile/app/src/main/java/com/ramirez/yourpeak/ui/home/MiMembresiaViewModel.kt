package com.ramirez.yourpeak.ui.home

import com.ramirez.yourpeak.data.model.MembresiaPeriodoResponse
import com.ramirez.yourpeak.data.repository.ResultadoHistorialMembresia
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    val periodos: List<MembresiaPeriodoResponse> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
) {
    /** Periodo vigente hoy; si no hay, el próximo programado. */
    val actual: MembresiaPeriodoResponse?
        get() = periodos.firstOrNull { it.estadoPeriodo.equals("VIGENTE", true) }
            ?: periodos.firstOrNull { it.estadoPeriodo.equals("PROGRAMADA", true) }

    /** Días que quedan hasta el vencimiento (incluido hoy) y duración total del periodo actual. */
    fun diasRestantes(hoy: LocalDate = LocalDate.now()): Pair<Int, Int>? {
        val periodo = actual ?: return null
        val inicio = fechaIso(periodo.fechaInicio) ?: return null
        val fin = fechaIso(periodo.fechaVencimiento) ?: return null
        val total = (ChronoUnit.DAYS.between(inicio, fin) + 1).toInt().coerceAtLeast(1)
        val desde = if (hoy.isBefore(inicio)) inicio else hoy
        val restantes = (ChronoUnit.DAYS.between(desde, fin) + 1).toInt().coerceIn(0, total)
        return restantes to total
    }

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
        viewModelScope.launch {
            when (val resultado = repository.historial(id)) {
                is ResultadoHistorialMembresia.Exito -> _estado.update { it.copy(periodos = resultado.periodos) }
                is ResultadoHistorialMembresia.Error -> _estado.update { it.copy(error = it.error ?: resultado.mensaje) }
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

/** Fecha ISO (yyyy-MM-dd) a LocalDate, o null si no se puede leer. */
internal fun fechaIso(texto: String?): LocalDate? = runCatching { LocalDate.parse(texto?.take(10)) }.getOrNull()
