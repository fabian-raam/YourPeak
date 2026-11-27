package com.ramirez.yourpeak.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ramirez.yourpeak.YourPeakApp
import com.ramirez.yourpeak.data.model.GoogleAuthResponse
import com.ramirez.yourpeak.data.model.GoogleChallenge
import com.ramirez.yourpeak.data.model.GoogleCompleteRequest
import com.ramirez.yourpeak.data.model.GoogleProfile
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.data.repository.ApiResult
import com.ramirez.yourpeak.data.repository.AuthRepository
import com.ramirez.yourpeak.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class GooglePending(val flowId: String, val profile: GoogleProfile, val link: Boolean)

internal data class GoogleAccessState(
    val busy: Boolean = false,
    val message: String? = null,
    val pending: GooglePending? = null,
    val user: LoginResponse? = null
)

internal class GoogleAccessViewModel(
    private val repository: AuthRepository,
    private val session: SessionManager
) : ViewModel() {
    private val state = MutableStateFlow(GoogleAccessState())
    val ui = state.asStateFlow()

    suspend fun start(pickCredential: suspend (GoogleChallenge) -> String?) {
        if (state.value.busy) return
        state.value = GoogleAccessState(busy = true)
        try {
            when (val result = repository.googleChallenge()) {
                is ApiResult.Error -> state.update { it.copy(message = result.mensaje) }
                is ApiResult.Exito -> {
                    val challenge = result.datos
                    val credential = pickCredential(challenge) ?: return
                    receive(repository.googleLogin(challenge.nonce, credential))
                }
            }
        } finally {
            state.update { it.copy(busy = false) }
        }
    }

    fun credentialError(message: String) {
        state.update { it.copy(message = message) }
    }

    fun complete(request: GoogleCompleteRequest) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                receive(repository.googleComplete(request))
            } finally {
                state.update { it.copy(busy = false) }
            }
        }
    }

    private fun receive(result: ApiResult<GoogleAuthResponse>) {
        when (result) {
            is ApiResult.Error -> state.update {
                it.copy(message = result.mensaje, pending = if (result.estadoHttp == 410) null else it.pending)
            }
            is ApiResult.Exito -> {
                val data = result.datos
                val user = data.usuario
                val flowId = data.flowId
                val profile = data.perfil
                when {
                    data.estado == "autenticado" && user?.idUsuario != null -> {
                        session.guardar(user)
                        state.update { it.copy(user = user, pending = null) }
                    }
                    (data.estado == "completar" || data.estado == "vincular") && flowId != null && profile != null ->
                        state.update { it.copy(pending = GooglePending(flowId, profile, data.estado == "vincular")) }
                    else -> credentialError("El servidor devolvió una respuesta inválida.")
                }
            }
        }
    }

    fun dismiss() {
        if (!state.value.busy) state.value = GoogleAccessState()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as YourPeakApp
                GoogleAccessViewModel(app.authRepository, app.sessionManager)
            }
        }
    }
}
