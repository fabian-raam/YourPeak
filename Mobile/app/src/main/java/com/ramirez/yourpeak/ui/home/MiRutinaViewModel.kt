package com.ramirez.yourpeak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramirez.yourpeak.data.model.DiaRutinaRequest
import com.ramirez.yourpeak.data.model.EjercicioRutinaRequest
import com.ramirez.yourpeak.data.model.EjercicioResponse
import com.ramirez.yourpeak.data.model.RutinaRequest
import com.ramirez.yourpeak.data.repository.EjercicioRepository
import com.ramirez.yourpeak.data.repository.ResultadoEjercicios
import com.ramirez.yourpeak.data.repository.ResultadoGeneracion
import com.ramirez.yourpeak.data.repository.ResultadoRutina
import com.ramirez.yourpeak.data.repository.RutinaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val NOMBRES_DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

private fun diasRutinaVacios() = NOMBRES_DIAS.mapIndexed { indice, nombre ->
    DiaRutinaUi(indice + 1, nombre)
}

data class EjercicioRutinaUi(
    val idEjercicio: Int,
    val nombre: String,
    val grupoMuscular: String,
    val tipoMedicion: String,
    val series: Int,
    val repeticiones: Int?,
    val segundos: Int?,
    val descansoSegundos: Int,
    /** IA si lo propuso el generador de rutinas; se conserva al guardar desde el móvil. */
    val origen: String = ORIGEN_USUARIO
)

const val ORIGEN_USUARIO = "USUARIO"
const val ORIGEN_IA = "IA"

data class DiaRutinaUi(
    val numero: Int,
    val nombre: String,
    val disponible: Boolean = false,
    val horaInicio: String? = null,
    val horaFin: String? = null,
    val ejercicios: List<EjercicioRutinaUi> = emptyList()
)

/** Propuesta mostrada en pantalla; fuente es GEMINI o REGLAS. */
data class PropuestaIa(val fuente: String, val mensaje: String) {
    val conGemini: Boolean get() = fuente == "GEMINI"
}

data class MiRutinaUiState(
    val dias: List<DiaRutinaUi> = diasRutinaVacios(),
    val diasGuardados: List<DiaRutinaUi> = diasRutinaVacios(),
    val catalogo: List<EjercicioResponse> = emptyList(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val generando: Boolean = false,
    val propuesta: PropuestaIa? = null,
    val error: String? = null,
    val mensaje: String? = null,
    val diaSeleccionado: Int = 1,
    val idEjercicioSeleccionado: Int? = null,
    val series: String = "3",
    val valorMedicion: String = "10",
    val descanso: String = "60"
) {
    val diaActual: DiaRutinaUi? get() = dias.firstOrNull { it.numero == diaSeleccionado }
    val hayCambios: Boolean get() = dias != diasGuardados
    val puedeGenerar: Boolean get() = !cargando && !guardando && !generando && dias.any { it.disponible }
}

class MiRutinaViewModel(
    private val rutinaRepository: RutinaRepository,
    private val ejercicioRepository: EjercicioRepository,
    private val idUsuario: Long?
) : ViewModel() {
    private val _estado = MutableStateFlow(MiRutinaUiState())
    val estado: StateFlow<MiRutinaUiState> = _estado.asStateFlow()

    init { cargar() }

    private fun cargar() {
        val id = idUsuario
        if (id == null) {
            _estado.update { it.copy(cargando = false, error = "No se identificó la cuenta.") }
            return
        }
        viewModelScope.launch {
            val rutinaResultado = rutinaRepository.consultar(id)
            val catalogoResultado = ejercicioRepository.listarDisponibles(id)

            val errorRutina = (rutinaResultado as? ResultadoRutina.Error)?.mensaje
            val errorCatalogo = (catalogoResultado as? ResultadoEjercicios.Error)?.mensaje
            val error = errorRutina ?: errorCatalogo

            val dias = (rutinaResultado as? ResultadoRutina.Exito)?.rutina?.dias
                ?.let(::convertirDias)
                ?: diasRutinaVacios()
            val catalogo = (catalogoResultado as? ResultadoEjercicios.Exito)?.ejercicios.orEmpty()

            _estado.update {
                it.copy(
                    dias = dias,
                    diasGuardados = dias,
                    catalogo = catalogo,
                    cargando = false,
                    error = error,
                    diaSeleccionado = dias.firstOrNull { dia -> dia.disponible }?.numero ?: 1
                )
            }
        }
    }

    fun reintentar() {
        _estado.update { it.copy(cargando = true, error = null) }
        cargar()
    }

    fun seleccionarDia(numero: Int) = _estado.update {
        it.copy(
            diaSeleccionado = numero,
            idEjercicioSeleccionado = null,
            series = "3",
            valorMedicion = "10",
            descanso = "60",
            error = null,
            mensaje = null
        )
    }

    fun seleccionarEjercicio(id: Int?) = _estado.update { estado ->
        val ejercicio = estado.catalogo.firstOrNull { it.idEjercicio == id }
        estado.copy(
            idEjercicioSeleccionado = id,
            valorMedicion = if (ejercicio?.tipoMedicion.equals("TIEMPO", ignoreCase = true)) "45" else "10",
            error = null
        )
    }

    fun cambiarSeries(valor: String) = _estado.update { it.copy(series = valor.filter(Char::isDigit)) }
    fun cambiarValorMedicion(valor: String) = _estado.update { it.copy(valorMedicion = valor.filter(Char::isDigit)) }
    fun cambiarDescanso(valor: String) = _estado.update { it.copy(descanso = valor.filter(Char::isDigit)) }

    fun agregarEjercicio() {
        val estado = _estado.value
        val dia = estado.diaActual
        if (dia == null || !dia.disponible) {
            _estado.update { it.copy(error = "Selecciona un día disponible para asignar ejercicios.") }
            return
        }
        val ejercicio = estado.catalogo.firstOrNull { it.idEjercicio == estado.idEjercicioSeleccionado }
        val idEjercicio = ejercicio?.idEjercicio
        if (ejercicio == null || idEjercicio == null) {
            _estado.update { it.copy(error = "Selecciona un ejercicio del catálogo.") }
            return
        }
        if (dia.ejercicios.any { it.idEjercicio == idEjercicio }) {
            _estado.update { it.copy(error = "Ese ejercicio ya está agregado a este día.") }
            return
        }
        val series = estado.series.toIntOrNull()
        val valor = estado.valorMedicion.toIntOrNull()
        val descanso = estado.descanso.toIntOrNull()
        val esTiempo = ejercicio.tipoMedicion.equals("TIEMPO", ignoreCase = true)
        if (series == null || series !in 1..100 || descanso == null || descanso !in 0..32767 ||
            valor == null || (if (esTiempo) valor !in 1..32767 else valor !in 1..127)
        ) {
            _estado.update { it.copy(error = "Revisa series, duración o repeticiones y descanso.") }
            return
        }

        val tipoMedicion = ejercicio.tipoMedicion.orEmpty()
        if (!esTiempo && !tipoMedicion.equals("REPETICIONES", ignoreCase = true)) {
            _estado.update { it.copy(error = "El tipo de medición del ejercicio no es compatible.") }
            return
        }

        val nuevo = EjercicioRutinaUi(
            idEjercicio = idEjercicio,
            nombre = ejercicio.nombre.orEmpty(),
            grupoMuscular = ejercicio.grupoMuscular.orEmpty(),
            tipoMedicion = tipoMedicion,
            series = series,
            repeticiones = if (esTiempo) null else valor,
            segundos = if (esTiempo) valor else null,
            descansoSegundos = descanso
        )
        _estado.update { actual ->
            actual.copy(
                dias = actual.dias.map { item ->
                    if (item.numero == dia.numero) item.copy(ejercicios = item.ejercicios + nuevo) else item
                },
                idEjercicioSeleccionado = null,
                series = "3",
                valorMedicion = "10",
                descanso = "60",
                error = null,
                mensaje = null
            )
        }
    }

    fun quitarEjercicio(idEjercicio: Int) = _estado.update { estado ->
        estado.copy(
            dias = estado.dias.map { dia ->
                if (dia.numero == estado.diaSeleccionado) {
                    dia.copy(ejercicios = dia.ejercicios.filterNot { it.idEjercicio == idEjercicio })
                } else dia
            },
            error = null,
            mensaje = null
        )
    }

    fun restablecer() = _estado.update {
        it.copy(dias = it.diasGuardados, propuesta = null, error = null, mensaje = null)
    }

    /** La propuesta reemplaza el borrador en pantalla; solo se guarda con «Guardar». */
    fun generar() {
        val id = idUsuario ?: run {
            _estado.update { it.copy(error = "No se identificó la cuenta.") }
            return
        }
        if (!_estado.value.puedeGenerar) return
        _estado.update { it.copy(generando = true, error = null, mensaje = null, idEjercicioSeleccionado = null) }
        viewModelScope.launch {
            when (val resultado = rutinaRepository.generar(id)) {
                is ResultadoGeneracion.Exito -> {
                    val dias = convertirDias(resultado.propuesta.dias.orEmpty())
                    _estado.update { actual ->
                        actual.copy(
                            dias = dias,
                            generando = false,
                            propuesta = PropuestaIa(
                                fuente = resultado.propuesta.fuente.orEmpty(),
                                mensaje = resultado.propuesta.mensaje.orEmpty()
                            ),
                            diaSeleccionado = dias.firstOrNull { it.ejercicios.isNotEmpty() }?.numero
                                ?: actual.diaSeleccionado
                        )
                    }
                }
                is ResultadoGeneracion.Error -> _estado.update {
                    it.copy(generando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun cerrarMensaje() = _estado.update { it.copy(mensaje = null) }

    fun guardar() {
        val id = idUsuario ?: run {
            _estado.update { it.copy(error = "No se identificó la cuenta.") }
            return
        }
        val estado = _estado.value
        if (estado.guardando || estado.generando) return
        if (estado.dias.any { !it.disponible && it.ejercicios.isNotEmpty() }) {
            _estado.update { it.copy(error = "Solo puedes asignar ejercicios a días disponibles.") }
            return
        }
        val request = RutinaRequest(
            dias = estado.dias.map { dia ->
                DiaRutinaRequest(
                    diaSemana = dia.numero,
                    ejercicios = dia.ejercicios.map { ejercicio ->
                        EjercicioRutinaRequest(
                            idEjercicio = ejercicio.idEjercicio,
                            series = ejercicio.series,
                            repeticiones = ejercicio.repeticiones,
                            segundos = ejercicio.segundos,
                            descansoSegundos = ejercicio.descansoSegundos,
                            origen = ejercicio.origen
                        )
                    }
                )
            }
        )
        _estado.update { it.copy(guardando = true, error = null, mensaje = null) }
        viewModelScope.launch {
            when (val resultado = rutinaRepository.guardar(id, request)) {
                is ResultadoRutina.Exito -> {
                    val dias = convertirDias(resultado.rutina.dias.orEmpty())
                    _estado.update {
                        it.copy(
                            dias = dias,
                            diasGuardados = dias,
                            guardando = false,
                            propuesta = null,
                            mensaje = "Rutina guardada correctamente.",
                            error = null
                        )
                    }
                }
                is ResultadoRutina.Error -> _estado.update {
                    it.copy(guardando = false, error = resultado.mensaje)
                }
            }
        }
    }

    private fun convertirDias(respuesta: List<com.ramirez.yourpeak.data.model.DiaRutinaResponse>): List<DiaRutinaUi> =
        (1..7).map { numero ->
            val remoto = respuesta.firstOrNull { it.diaSemana == numero }
            DiaRutinaUi(
                numero = numero,
                nombre = NOMBRES_DIAS[numero - 1],
                disponible = remoto?.disponible == true,
                horaInicio = remoto?.horaInicio?.take(5),
                horaFin = remoto?.horaFin?.take(5),
                ejercicios = remoto?.ejercicios.orEmpty().mapNotNull { ejercicio ->
                    val id = ejercicio.idEjercicio ?: return@mapNotNull null
                    EjercicioRutinaUi(
                        idEjercicio = id,
                        nombre = ejercicio.nombre.orEmpty(),
                        grupoMuscular = ejercicio.grupoMuscular.orEmpty(),
                        tipoMedicion = ejercicio.tipoMedicion.orEmpty(),
                        series = ejercicio.series ?: 3,
                        repeticiones = ejercicio.repeticiones,
                        segundos = ejercicio.segundos,
                        descansoSegundos = ejercicio.descansoSegundos ?: 0,
                        origen = if (ejercicio.origen == ORIGEN_IA) ORIGEN_IA else ORIGEN_USUARIO
                    )
                }
            )
        }

    companion object {
        fun factory(
            rutinaRepository: RutinaRepository,
            ejercicioRepository: EjercicioRepository,
            idUsuario: Long?
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(MiRutinaViewModel::class.java))
                return MiRutinaViewModel(
                    rutinaRepository, ejercicioRepository, idUsuario
                ) as T
            }
        }
    }
}
