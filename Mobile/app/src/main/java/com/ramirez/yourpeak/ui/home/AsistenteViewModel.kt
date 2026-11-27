package com.ramirez.yourpeak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramirez.yourpeak.data.model.MensajeAsistente
import com.ramirez.yourpeak.data.repository.AsistenteRepository
import com.ramirez.yourpeak.data.repository.ResultadoAsistente
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Un mensaje del chat; enTema=false y error marcan respuestas que no se reenvían como contexto. */
data class MensajeChat(
    val deUsuario: Boolean,
    val texto: String,
    val enTema: Boolean = true,
    val error: Boolean = false
)

data class AsistenteUiState(
    val mensajes: List<MensajeChat> = emptyList(),
    val pregunta: String = "",
    val enviando: Boolean = false,
    val restantes: Int? = null,
    val limite: Int? = null
)

class AsistenteViewModel(
    private val repository: AsistenteRepository,
    private val idUsuario: Long?
) : ViewModel() {
    private val _estado = MutableStateFlow(AsistenteUiState())
    val estado: StateFlow<AsistenteUiState> = _estado.asStateFlow()

    fun cambiarPregunta(texto: String) = _estado.update { it.copy(pregunta = texto.take(500)) }

    fun nuevaConversacion() = _estado.update { AsistenteUiState(restantes = it.restantes, limite = it.limite) }

    fun enviar(texto: String = _estado.value.pregunta) {
        val pregunta = texto.trim()
        val actual = _estado.value
        val id = idUsuario
        if (pregunta.isEmpty() || actual.enviando || id == null) return
        val historial = historialValido(actual.mensajes)
        _estado.update { it.copy(mensajes = it.mensajes + MensajeChat(true, pregunta), pregunta = "", enviando = true) }
        viewModelScope.launch {
            val mensaje = when (val resultado = repository.preguntar(id, pregunta, historial)) {
                is ResultadoAsistente.Exito -> {
                    _estado.update { it.copy(restantes = resultado.respuesta.consultasRestantesHoy, limite = resultado.respuesta.limiteDiario) }
                    MensajeChat(false, resultado.respuesta.respuesta.orEmpty(), enTema = resultado.respuesta.enTema)
                }
                is ResultadoAsistente.Error -> MensajeChat(false, resultado.mensaje, error = true)
            }
            _estado.update { it.copy(mensajes = it.mensajes + mensaje, enviando = false) }
        }
    }

    companion object {
        /** Solo los pares pregunta-respuesta válidos se envían como contexto. */
        fun historialValido(mensajes: List<MensajeChat>): List<MensajeAsistente> =
            mensajes.filterIndexed { indice, mensaje ->
                if (mensaje.deUsuario) mensajes.getOrNull(indice + 1)?.let { !it.deUsuario && !it.error && it.enTema } == true
                else !mensaje.error && mensaje.enTema
            }.takeLast(12).map { MensajeAsistente(if (it.deUsuario) "usuario" else "asistente", it.texto) }

        fun factory(repository: AsistenteRepository, idUsuario: Long?) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AsistenteViewModel::class.java))
                return AsistenteViewModel(repository, idUsuario) as T
            }
        }
    }
}
