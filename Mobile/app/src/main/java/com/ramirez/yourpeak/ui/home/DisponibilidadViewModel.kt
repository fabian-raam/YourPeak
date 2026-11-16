package com.ramirez.yourpeak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramirez.yourpeak.data.model.DiaDisponibilidadRequest
import com.ramirez.yourpeak.data.model.DisponibilidadRequest
import com.ramirez.yourpeak.data.repository.DisponibilidadRepository
import com.ramirez.yourpeak.data.repository.ResultadoDisponibilidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")
private val DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

private fun diasSemanales() = DIAS.mapIndexed { indice, nombre ->
    DiaDisponibilidadUi(numero = indice + 1, nombre = nombre)
}

private fun parsearHora(hora: String): LocalTime? = try {
    LocalTime.parse(hora.take(5), FORMATO_HORA)
} catch (_: RuntimeException) {
    null
}

private fun horaApi(hora: String): String = "${hora.take(5)}:00"

data class DiaDisponibilidadUi(
    val numero: Int,
    val nombre: String,
    val disponible: Boolean = false,
    val horaInicio: String? = null,
    val horaFin: String? = null
)

data class DisponibilidadUiState(
    val dias: List<DiaDisponibilidadUi> = diasSemanales(),
    val diasGuardados: List<DiaDisponibilidadUi> = diasSemanales(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
) {
    val cantidadDiasDisponibles: Int get() = dias.count { it.disponible }
    val hayCambios: Boolean get() = dias != diasGuardados
    val minutosSemanales: Int
        get() = dias.filter { it.disponible }.sumOf { dia ->
            val inicio = dia.horaInicio?.let(::parsearHora)
            val fin = dia.horaFin?.let(::parsearHora)
            if (inicio == null || fin == null || !fin.isAfter(inicio)) 0
            else (fin.toSecondOfDay() - inicio.toSecondOfDay()) / 60
        }
}

class DisponibilidadViewModel(
    private val repository: DisponibilidadRepository,
    private val idUsuario: Long?
) : ViewModel() {
    private val _estado = MutableStateFlow(DisponibilidadUiState())
    val estado: StateFlow<DisponibilidadUiState> = _estado.asStateFlow()

    init { cargar() }

    private fun cargar() {
        val id = idUsuario
        if (id == null) {
            _estado.update { it.copy(cargando = false, error = "No se identificó la cuenta.") }
            return
        }
        viewModelScope.launch {
            when (val resultado = repository.consultar(id)) {
                is ResultadoDisponibilidad.Exito -> {
                    val dias = convertirDias(resultado.disponibilidad.dias.orEmpty())
                    _estado.update {
                        it.copy(dias = dias, diasGuardados = dias, cargando = false, error = null)
                    }
                }
                is ResultadoDisponibilidad.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun reintentar() {
        _estado.update { it.copy(cargando = true, error = null, mensaje = null) }
        cargar()
    }

    fun cambiarDisponible(numero: Int, disponible: Boolean) = _estado.update { estado ->
        estado.copy(
            dias = estado.dias.map { dia ->
                if (dia.numero != numero) dia
                else dia.copy(
                    disponible = disponible,
                    horaInicio = if (disponible) dia.horaInicio else null,
                    horaFin = if (disponible) dia.horaFin else null
                )
            },
            error = null,
            mensaje = null
        )
    }

    fun cambiarHora(numero: Int, esInicio: Boolean, hora: String) = _estado.update { estado ->
        estado.copy(
            dias = estado.dias.map { dia ->
                if (dia.numero != numero) dia
                else if (esInicio) dia.copy(horaInicio = hora) else dia.copy(horaFin = hora)
            },
            error = null,
            mensaje = null
        )
    }

    fun restablecer() = _estado.update {
        it.copy(dias = it.diasGuardados, error = null, mensaje = null)
    }

    fun cerrarMensaje() = _estado.update { it.copy(mensaje = null) }

    fun guardar() {
        val errorValidacion = validar(_estado.value.dias)
        if (errorValidacion != null) {
            _estado.update { it.copy(error = errorValidacion, mensaje = null) }
            return
        }
        val id = idUsuario ?: run {
            _estado.update { it.copy(error = "No se identificó la cuenta.") }
            return
        }
        val datos = _estado.value.dias.map { dia ->
            DiaDisponibilidadRequest(
                diaSemana = dia.numero,
                disponible = dia.disponible,
                horaInicio = if (dia.disponible) dia.horaInicio?.let(::horaApi) else null,
                horaFin = if (dia.disponible) dia.horaFin?.let(::horaApi) else null
            )
        }
        _estado.update { it.copy(guardando = true, error = null, mensaje = null) }
        viewModelScope.launch {
            when (val resultado = repository.guardar(id, DisponibilidadRequest(datos))) {
                is ResultadoDisponibilidad.Exito -> {
                    val diasGuardados = convertirDias(resultado.disponibilidad.dias.orEmpty())
                    _estado.update {
                        it.copy(
                            dias = diasGuardados,
                            diasGuardados = diasGuardados,
                            guardando = false,
                            mensaje = "Disponibilidad guardada correctamente.",
                            error = null
                        )
                    }
                }
                is ResultadoDisponibilidad.Error -> _estado.update {
                    it.copy(guardando = false, error = resultado.mensaje)
                }
            }
        }
    }

    private fun convertirDias(respuesta: List<com.ramirez.yourpeak.data.model.DiaDisponibilidadResponse>): List<DiaDisponibilidadUi> =
        (1..7).map { numero ->
            val remoto = respuesta.firstOrNull { it.diaSemana == numero }
            val nombre = DIAS[numero - 1]
            val disponible = remoto?.disponible == true
            DiaDisponibilidadUi(
                numero = numero,
                nombre = nombre,
                disponible = disponible,
                horaInicio = if (disponible) remoto?.horaInicio?.take(5) else null,
                horaFin = if (disponible) remoto?.horaFin?.take(5) else null
            )
        }

    private fun validar(dias: List<DiaDisponibilidadUi>): String? {
        dias.filter { it.disponible }.forEach { dia ->
            val inicio = dia.horaInicio?.let(::parsearHora)
            val fin = dia.horaFin?.let(::parsearHora)
            if (inicio == null || fin == null) {
                return "Completa las horas de ${dia.nombre.lowercase()}."
            }
            if (!fin.isAfter(inicio)) {
                return "La hora final de ${dia.nombre.lowercase()} debe ser posterior a la inicial."
            }
        }
        return null
    }

    companion object {
        fun factory(repository: DisponibilidadRepository, idUsuario: Long?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DisponibilidadViewModel::class.java))
                    return DisponibilidadViewModel(repository, idUsuario) as T
                }
            }
    }
}
