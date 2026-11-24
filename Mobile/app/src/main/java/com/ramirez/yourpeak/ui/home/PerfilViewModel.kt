package com.ramirez.yourpeak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramirez.yourpeak.data.model.OpcionPerfilResponse
import com.ramirez.yourpeak.data.model.PerfilInicialRequest
import com.ramirez.yourpeak.data.model.PerfilInicialResponse
import com.ramirez.yourpeak.data.repository.PerfilRepository
import com.ramirez.yourpeak.data.repository.ResultadoPerfil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

/** Opción de un catálogo del perfil (objetivo, experiencia o actividad). */
data class OpcionPerfil(val id: Int, val nombre: String, val descripcion: String)

data class PerfilUiState(
    val objetivos: List<OpcionPerfil> = emptyList(),
    val nivelesExperiencia: List<OpcionPerfil> = emptyList(),
    val nivelesActividad: List<OpcionPerfil> = emptyList(),
    val idObjetivo: Int? = null,
    val idExperiencia: Int? = null,
    val idActividad: Int? = null,
    val sexo: String? = null,
    /** Sexo ya registrado en Spring; si existe, el campo queda bloqueado como en la web. */
    val sexoGuardado: String? = null,
    val peso: String = "",
    val talla: String = "",
    val completado: Boolean = false,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val errorCarga: String? = null,
    val error: String? = null,
    val aviso: String? = null
)

/** Consulta y guarda el perfil inicial con las mismas reglas que el formulario web. */
class PerfilViewModel(
    private val repository: PerfilRepository,
    private val idUsuario: Long?
) : ViewModel() {
    private val _estado = MutableStateFlow(PerfilUiState())
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        val id = idUsuario ?: run {
            _estado.update { it.copy(cargando = false, errorCarga = "No se identificó la cuenta.") }
            return
        }
        _estado.update { it.copy(cargando = true, errorCarga = null) }
        viewModelScope.launch {
            when (val resultado = repository.consultar(id)) {
                is ResultadoPerfil.Exito -> _estado.update { aplicar(it, resultado.perfil).copy(cargando = false) }
                is ResultadoPerfil.Error -> _estado.update { it.copy(cargando = false, errorCarga = resultado.mensaje) }
            }
        }
    }

    fun elegirObjetivo(id: Int) = _estado.update { it.copy(idObjetivo = id, error = null, aviso = null) }
    fun elegirExperiencia(id: Int) = _estado.update { it.copy(idExperiencia = id, error = null, aviso = null) }
    fun elegirActividad(id: Int) = _estado.update { it.copy(idActividad = id, error = null, aviso = null) }

    fun elegirSexo(valor: String) = _estado.update {
        if (it.sexoGuardado != null) it else it.copy(sexo = valor, error = null, aviso = null)
    }

    fun cambiarPeso(valor: String) = _estado.update { it.copy(peso = filtrarDecimal(valor), error = null, aviso = null) }
    fun cambiarTalla(valor: String) = _estado.update { it.copy(talla = filtrarDecimal(valor), error = null, aviso = null) }
    fun cerrarAviso() = _estado.update { it.copy(aviso = null) }

    fun guardar() {
        val id = idUsuario ?: return
        val actual = _estado.value
        if (actual.guardando) return
        val objetivo = actual.idObjetivo
        val experiencia = actual.idExperiencia
        val actividad = actual.idActividad
        val sexo = actual.sexoGuardado ?: actual.sexo
        val peso = medida(actual.peso)
        val talla = medida(actual.talla)
        val error = when {
            objetivo == null -> "Selecciona tu objetivo."
            experiencia == null -> "Selecciona tu nivel de experiencia."
            actividad == null -> "Selecciona tu nivel de actividad."
            sexo == null -> "Selecciona tu sexo."
            peso == null -> "Ingresa un peso entre 0.01 y 999.99 kg, con hasta dos decimales."
            talla == null -> "Ingresa una talla entre 0.01 y 999.99 cm, con hasta dos decimales."
            else -> null
        }
        if (error != null) {
            _estado.update { it.copy(error = error, aviso = null) }
            return
        }
        _estado.update { it.copy(guardando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val request = PerfilInicialRequest(objetivo!!, experiencia!!, actividad!!, sexo!!, peso!!, talla!!)
            when (val resultado = repository.guardar(id, request)) {
                is ResultadoPerfil.Exito -> _estado.update {
                    aplicar(it, resultado.perfil).copy(guardando = false, aviso = "Tu perfil se guardó correctamente.")
                }
                is ResultadoPerfil.Error -> _estado.update { it.copy(guardando = false, error = resultado.mensaje) }
            }
        }
    }

    private fun aplicar(estado: PerfilUiState, perfil: PerfilInicialResponse): PerfilUiState {
        val catalogos = perfil.catalogos
        return estado.copy(
            objetivos = catalogos?.objetivos?.let(::opciones) ?: estado.objetivos,
            nivelesExperiencia = catalogos?.nivelesExperiencia?.let(::opciones) ?: estado.nivelesExperiencia,
            nivelesActividad = catalogos?.nivelesActividad?.let(::opciones) ?: estado.nivelesActividad,
            idObjetivo = perfil.idTipoObjetivo,
            idExperiencia = perfil.idNivelExperiencia,
            idActividad = perfil.idNivelActividad,
            sexo = perfil.sexo,
            sexoGuardado = perfil.sexo,
            peso = perfil.pesoKg?.stripTrailingZeros()?.toPlainString().orEmpty(),
            talla = perfil.tallaCm?.stripTrailingZeros()?.toPlainString().orEmpty(),
            completado = perfil.completado == true
        )
    }

    private fun opciones(lista: List<OpcionPerfilResponse>) = lista.mapNotNull { opcion ->
        opcion.id?.let { OpcionPerfil(it, opcion.nombre.orEmpty(), opcion.descripcion.orEmpty()) }
    }

    companion object {
        val MAXIMO: BigDecimal = BigDecimal("999.99")
        val MINIMO: BigDecimal = BigDecimal("0.01")

        /** Devuelve la medida con dos decimales o null si no cumple las reglas de Spring. */
        fun medida(texto: String): BigDecimal? {
            val valor = texto.replace(',', '.').toBigDecimalOrNull() ?: return null
            val normalizado = runCatching { valor.setScale(2, RoundingMode.UNNECESSARY) }.getOrNull() ?: return null
            return normalizado.takeIf { it in MINIMO..MAXIMO }
        }

        private fun filtrarDecimal(valor: String) = valor.filter { it.isDigit() || it == '.' || it == ',' }.take(7)

        fun factory(repository: PerfilRepository, idUsuario: Long?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(PerfilViewModel::class.java))
                    return PerfilViewModel(repository, idUsuario) as T
                }
            }
    }
}
