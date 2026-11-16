package com.ramirez.yourpeak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramirez.yourpeak.data.model.AlimentoNutricionResponse
import com.ramirez.yourpeak.data.model.IngestaNutricionResponse
import com.ramirez.yourpeak.data.model.RegistrarIngestaRequest
import com.ramirez.yourpeak.data.model.ResumenNutricionalResponse
import com.ramirez.yourpeak.data.repository.NutricionRepository
import com.ramirez.yourpeak.data.repository.ResultadoNutricion
import com.ramirez.yourpeak.domain.Fechas
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IngestaNutricionUi(
    val idIngesta: Long,
    val idAlimento: Int,
    val alimento: String,
    val cantidadG: BigDecimal,
    val fechaHora: String?,
    val kcal: BigDecimal,
    val proteinaG: BigDecimal,
    val carbohidratoG: BigDecimal,
    val grasaG: BigDecimal
)

data class NutricionUiState(
    val alimentos: List<AlimentoNutricionResponse> = emptyList(),
    val ingestas: List<IngestaNutricionUi> = emptyList(),
    val fecha: String = Fechas.hoyIso(),
    val hoy: String = Fechas.hoyIso(),
    val kcal: BigDecimal = BigDecimal.ZERO,
    val proteinaG: BigDecimal = BigDecimal.ZERO,
    val carbohidratoG: BigDecimal = BigDecimal.ZERO,
    val grasaG: BigDecimal = BigDecimal.ZERO,
    val cargandoCatalogo: Boolean = true,
    val cargandoDia: Boolean = true,
    val guardando: Boolean = false,
    val busqueda: String = "",
    val idAlimentoSeleccionado: Int? = null,
    val cantidadG: String = "100",
    val idIngestaEditando: Long? = null,
    val cantidadEditadaG: String = "",
    val idIngestaPorEliminar: Long? = null,
    val error: String? = null,
    val errorCatalogo: String? = null,
    val aviso: String? = null
) {
    val esHoy: Boolean get() = fecha == hoy
    val alimentoSeleccionado: AlimentoNutricionResponse?
        get() = alimentos.firstOrNull { it.idAlimento == idAlimentoSeleccionado }
}

class NutricionViewModel(
    private val repository: NutricionRepository,
    private val idUsuario: Long?
) : ViewModel() {
    private val _estado = MutableStateFlow(NutricionUiState())
    val estado: StateFlow<NutricionUiState> = _estado.asStateFlow()

    init { cargarInicial() }

    private fun cargarInicial() {
        val id = idUsuario
        if (id == null) {
            _estado.update {
                it.copy(cargandoCatalogo = false, cargandoDia = false,
                    error = "No se pudo identificar la cuenta.")
            }
            return
        }
        viewModelScope.launch {
            when (val resultado = repository.listarAlimentos(id)) {
                is ResultadoNutricion.Exito -> _estado.update {
                    it.copy(alimentos = resultado.dato, cargandoCatalogo = false, errorCatalogo = null)
                }
                is ResultadoNutricion.Error -> _estado.update {
                    it.copy(cargandoCatalogo = false, errorCatalogo = resultado.mensaje)
                }
            }
            cargarDia(id, _estado.value.fecha)
        }
    }

    fun reintentarCatalogo() {
        val id = idUsuario ?: return
        _estado.update { it.copy(cargandoCatalogo = true, errorCatalogo = null, error = null) }
        viewModelScope.launch {
            when (val resultado = repository.listarAlimentos(id)) {
                is ResultadoNutricion.Exito -> _estado.update {
                    it.copy(alimentos = resultado.dato, cargandoCatalogo = false, errorCatalogo = null)
                }
                is ResultadoNutricion.Error -> _estado.update {
                    it.copy(cargandoCatalogo = false, errorCatalogo = resultado.mensaje)
                }
            }
        }
    }

    fun actualizarDia(fecha: String = _estado.value.fecha) {
        if (fecha > _estado.value.hoy || !fecha.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return
        _estado.update {
            it.copy(fecha = fecha, cargandoDia = true, ingestas = emptyList(),
                error = null, aviso = null, idIngestaEditando = null)
        }
        val id = idUsuario ?: return
        viewModelScope.launch { cargarDia(id, fecha) }
    }

    fun moverDia(diferencia: Int) {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val fecha = runCatching { formato.parse(_estado.value.fecha) }.getOrNull() ?: return
        val calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
            time = fecha
            add(Calendar.DAY_OF_MONTH, diferencia)
        }
        val nueva = formato.format(Date(calendario.timeInMillis))
        actualizarDia(nueva)
    }

    fun buscar(valor: String) = _estado.update { it.copy(busqueda = valor) }
    fun seleccionarAlimento(id: Int?) = _estado.update { it.copy(idAlimentoSeleccionado = id, error = null) }
    fun cambiarCantidad(valor: String) = _estado.update {
        it.copy(cantidadG = valor.filter { char -> char.isDigit() || char == '.' || char == ',' })
    }

    fun registrar() {
        val estado = _estado.value
        val id = idUsuario
        val alimento = estado.alimentoSeleccionado
        val request = crearRequest(alimento?.idAlimento, estado.cantidadG)
        if (id == null || !estado.esHoy || alimento == null || request == null) {
            _estado.update { it.copy(error = "Elige un alimento y una cantidad válida mayor que cero.") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null, aviso = null) }
            when (val resultado = repository.registrar(id, request)) {
                is ResultadoNutricion.Exito -> {
                    _estado.update { it.copy(guardando = false, idAlimentoSeleccionado = null,
                        cantidadG = "100", busqueda = "", aviso = "Alimento agregado al registro del día.") }
                    cargarDia(id, _estado.value.fecha)
                }
                is ResultadoNutricion.Error -> _estado.update {
                    it.copy(guardando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun iniciarEdicion(ingesta: IngestaNutricionUi) = _estado.update {
        it.copy(idIngestaEditando = ingesta.idIngesta,
            cantidadEditadaG = ingesta.cantidadG.stripTrailingZeros().toPlainString(), error = null)
    }

    fun cambiarCantidadEditada(valor: String) = _estado.update {
        it.copy(cantidadEditadaG = valor.filter { char -> char.isDigit() || char == '.' || char == ',' })
    }

    fun cancelarEdicion() = _estado.update { it.copy(idIngestaEditando = null, error = null) }

    fun guardarEdicion(ingesta: IngestaNutricionUi) {
        val id = idUsuario
        val estado = _estado.value
        val request = crearRequest(ingesta.idAlimento, estado.cantidadEditadaG)
        if (id == null || request == null) {
            _estado.update { it.copy(error = "La cantidad debe ser mayor que cero y admitir hasta dos decimales.") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null, aviso = null) }
            when (val resultado = repository.actualizar(id, ingesta.idIngesta, request)) {
                is ResultadoNutricion.Exito -> {
                    _estado.update { it.copy(guardando = false, idIngestaEditando = null,
                        aviso = "Se actualizó la cantidad del consumo.") }
                    cargarDia(id, _estado.value.fecha)
                }
                is ResultadoNutricion.Error -> _estado.update {
                    it.copy(guardando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun pedirEliminar(ingesta: IngestaNutricionUi?) = _estado.update {
        it.copy(idIngestaPorEliminar = ingesta?.idIngesta)
    }

    fun confirmarEliminar() {
        val id = idUsuario ?: return
        val idIngesta = _estado.value.idIngestaPorEliminar ?: return
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null, aviso = null) }
            when (val resultado = repository.eliminar(id, idIngesta)) {
                is ResultadoNutricion.Exito -> {
                    _estado.update { it.copy(guardando = false, idIngestaPorEliminar = null,
                        aviso = "Se eliminó el consumo del registro.") }
                    cargarDia(id, _estado.value.fecha)
                }
                is ResultadoNutricion.Error -> _estado.update {
                    it.copy(guardando = false, idIngestaPorEliminar = null, error = resultado.mensaje)
                }
            }
        }
    }

    fun cerrarMensajes() = _estado.update { it.copy(error = null, errorCatalogo = null, aviso = null) }

    private suspend fun cargarDia(id: Long, fecha: String) {
        _estado.update { it.copy(cargandoDia = true) }
        when (val resultado = repository.consultarDia(id, fecha)) {
            is ResultadoNutricion.Exito -> _estado.update {
                it.copy(cargandoDia = false, error = null).aplicarResumen(resultado.dato)
            }
            is ResultadoNutricion.Error -> _estado.update {
                it.copy(cargandoDia = false, error = resultado.mensaje)
            }
        }
    }

    private fun crearRequest(idAlimento: Int?, cantidad: String): RegistrarIngestaRequest? {
        val normalizada = cantidad.replace(',', '.').toBigDecimalOrNull() ?: return null
        if (idAlimento == null || normalizada <= BigDecimal.ZERO || normalizada > BigDecimal("99999.99")) return null
        val conDosDecimales = try {
            normalizada.setScale(2, RoundingMode.UNNECESSARY)
        } catch (_: ArithmeticException) {
            return null
        }
        return RegistrarIngestaRequest(idAlimento, conDosDecimales)
    }

    private fun NutricionUiState.aplicarResumen(resumen: ResumenNutricionalResponse) = copy(
        kcal = resumen.kcal ?: BigDecimal.ZERO,
        proteinaG = resumen.proteinaG ?: BigDecimal.ZERO,
        carbohidratoG = resumen.carbohidratoG ?: BigDecimal.ZERO,
        grasaG = resumen.grasaG ?: BigDecimal.ZERO,
        ingestas = resumen.ingestas.orEmpty().mapNotNull(::convertirIngesta)
    )

    private fun convertirIngesta(ingesta: IngestaNutricionResponse): IngestaNutricionUi? {
        val id = ingesta.idIngesta ?: return null
        val alimentoId = ingesta.idAlimento ?: return null
        return IngestaNutricionUi(
            idIngesta = id,
            idAlimento = alimentoId,
            alimento = ingesta.alimento.orEmpty(),
            cantidadG = ingesta.cantidadG ?: BigDecimal.ZERO,
            fechaHora = ingesta.fechaHora,
            kcal = ingesta.kcal ?: BigDecimal.ZERO,
            proteinaG = ingesta.proteinaG ?: BigDecimal.ZERO,
            carbohidratoG = ingesta.carbohidratoG ?: BigDecimal.ZERO,
            grasaG = ingesta.grasaG ?: BigDecimal.ZERO
        )
    }

    companion object {
        fun factory(repository: NutricionRepository, idUsuario: Long?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(NutricionViewModel::class.java))
                    return NutricionViewModel(repository, idUsuario) as T
                }
            }
    }
}
