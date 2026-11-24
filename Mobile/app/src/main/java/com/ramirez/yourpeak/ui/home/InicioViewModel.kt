package com.ramirez.yourpeak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramirez.yourpeak.data.model.DiaRutinaResponse
import com.ramirez.yourpeak.data.model.RegistroProgresoResponse
import com.ramirez.yourpeak.data.model.ResumenNutricionalResponse
import com.ramirez.yourpeak.data.model.RutinaResponse
import com.ramirez.yourpeak.data.repository.NutricionRepository
import com.ramirez.yourpeak.data.repository.ProgresoRepository
import com.ramirez.yourpeak.data.repository.ResultadoNutricion
import com.ramirez.yourpeak.data.repository.ResultadoProgreso
import com.ramirez.yourpeak.data.repository.ResultadoRutina
import com.ramirez.yourpeak.data.repository.RutinaRepository
import com.ramirez.yourpeak.domain.Fechas
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

data class InicioUiState(
    val rutina: RutinaResponse? = null,
    val nutricion: ResumenNutricionalResponse? = null,
    val ultimoPeso: RegistroProgresoResponse? = null,
    val rutinaCargando: Boolean = true,
    val nutricionCargando: Boolean = true,
    val progresoCargando: Boolean = true,
    val rutinaError: String? = null,
    val nutricionError: String? = null,
    val progresoError: String? = null
)

data class ProximoEntrenamientoUi(
    val dia: DiaRutinaResponse,
    val fechaIso: String,
    val diasHasta: Int
)

/** Carga los resúmenes que se muestran en Inicio, delegando el acceso HTTP a los repositorios. */
class InicioViewModel(
    private val rutinaRepository: RutinaRepository,
    private val nutricionRepository: NutricionRepository,
    private val progresoRepository: ProgresoRepository,
    private val idUsuario: Long?
) : ViewModel() {
    private val _estado = MutableStateFlow(InicioUiState())
    val estado: StateFlow<InicioUiState> = _estado.asStateFlow()

    fun actualizar() {
        val id = idUsuario
        if (id == null) {
            _estado.value = InicioUiState(
                rutinaCargando = false, nutricionCargando = false, progresoCargando = false,
                rutinaError = "No se identificó la cuenta.",
                nutricionError = "No se identificó la cuenta.",
                progresoError = "No se identificó la cuenta."
            )
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(rutinaCargando = true, rutinaError = null) }
            when (val resultado = rutinaRepository.consultar(id)) {
                is ResultadoRutina.Exito -> _estado.update {
                    it.copy(rutina = resultado.rutina, rutinaCargando = false, rutinaError = null)
                }
                is ResultadoRutina.Error -> _estado.update { it.copy(rutinaCargando = false, rutinaError = resultado.mensaje) }
            }
        }
        viewModelScope.launch {
            _estado.update { it.copy(nutricionCargando = true, nutricionError = null) }
            when (val resultado = nutricionRepository.consultarDia(id, Fechas.hoyIso())) {
                is ResultadoNutricion.Exito -> _estado.update {
                    it.copy(nutricion = resultado.dato, nutricionCargando = false, nutricionError = null)
                }
                is ResultadoNutricion.Error -> _estado.update { it.copy(nutricionCargando = false, nutricionError = resultado.mensaje) }
            }
        }
        viewModelScope.launch {
            _estado.update { it.copy(progresoCargando = true, progresoError = null) }
            when (val resultado = progresoRepository.consultar(id)) {
                is ResultadoProgreso.Exito -> _estado.update {
                    val ultimo = resultado.dato.maxByOrNull { registro -> registro.fechaRegistro.orEmpty() }
                    it.copy(ultimoPeso = ultimo, progresoCargando = false, progresoError = null)
                }
                is ResultadoProgreso.Error -> _estado.update { it.copy(progresoCargando = false, progresoError = resultado.mensaje) }
            }
        }
    }

    fun proximoEntrenamiento(): ProximoEntrenamientoUi? {
        val dias = _estado.value.rutina?.dias.orEmpty()
        val zonaLima = TimeZone.getTimeZone("America/Lima")
        val ahora = Calendar.getInstance(zonaLima)
        val hoySemana = ((ahora.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1 // ISO: lunes=1, domingo=7
        val fechaHoy = Fechas.hoyIso()
        val horaActual = "%02d:%02d".format(Locale.ROOT, ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE))
        val parseFecha = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }
        for (offset in 0..7) {
            val fecha = parseFecha.parse(fechaHoy) ?: return null
            val calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
                time = fecha
                add(Calendar.DAY_OF_MONTH, offset)
            }
            val semana = ((calendario.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
            val dia = dias.firstOrNull {
                it.diaSemana == semana && it.disponible == true && !it.ejercicios.isNullOrEmpty() &&
                    (offset > 0 || it.horaFin.orEmpty().take(5) > horaActual)
            }
            if (dia != null) return ProximoEntrenamientoUi(dia, parseFecha.format(calendario.time), offset)
        }
        return null
    }

    fun nombreDia(numero: Int): String = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        .getOrElse(numero - 1) { "Entrenamiento" }

    fun fechaAmigable(iso: String, patron: String = "EEEE d 'de' MMMM"): String {
        val fecha = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(iso)
        }.getOrNull() ?: return iso
        return SimpleDateFormat(patron, Locale.forLanguageTag("es-PE")).format(fecha).replaceFirstChar { it.uppercase() }
    }

    fun fechaPesoAmigable(fecha: String?): String {
        if (fecha.isNullOrBlank()) return ""
        val parseadores = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd")
        val date: Date = parseadores.firstNotNullOfOrNull { patron ->
            runCatching { SimpleDateFormat(patron, Locale.ROOT).parse(fecha) }.getOrNull()
        } ?: return fecha.take(10)
        return SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-PE")).format(date)
    }

    companion object {
        fun factory(rutina: RutinaRepository, nutricion: NutricionRepository, progreso: ProgresoRepository, id: Long?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(InicioViewModel::class.java))
                    return InicioViewModel(rutina, nutricion, progreso, id) as T
                }
            }
    }
}
