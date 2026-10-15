package com.frontendusuario.app.ui.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.frontendusuario.app.data.remote.RegistroRequest
import com.frontendusuario.app.data.repository.RegistroRepository
import com.frontendusuario.app.data.repository.RegistroResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistroViewModel(
    private val repository: RegistroRepository = RegistroRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun registrar(
        nombre: String,
        apellido: String,
        correo: String,
        contrasena: String,
    ) {
        if (_uiState.value.isLoading || _uiState.value.isSuccess) {
            return
        }

        val validationErrors = validateInput(nombre, apellido, correo, contrasena)
        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    fieldErrors = validationErrors,
                    generalError = null,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                fieldErrors = emptyMap(),
                generalError = null,
            )
        }

        viewModelScope.launch {
            val request = RegistroRequest(
                nombre = nombre.trim(),
                apellido = apellido.trim(),
                correo = correo.trim(),
                contrasena = contrasena,
            )

            when (val result = repository.registrar(request)) {
                is RegistroResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            fieldErrors = emptyMap(),
                            generalError = null,
                            successMessage = result.response.mensaje
                                ?: "Registro completado con éxito. Su cuenta está pendiente de aprobación.",
                        )
                    }
                }
                is RegistroResult.ValidationError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            fieldErrors = result.campos,
                            generalError = result.detail,
                        )
                    }
                }
                is RegistroResult.ConflictError -> {
                    val fieldErrors = if (result.detail != null) {
                        mapOf("correo" to result.detail)
                    } else {
                        mapOf("correo" to "El correo electrónico ya se encuentra registrado.")
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            fieldErrors = fieldErrors,
                            generalError = result.detail,
                        )
                    }
                }
                is RegistroResult.ConnectionError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.message,
                        )
                    }
                }
                is RegistroResult.TimeoutError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.message,
                        )
                    }
                }
                is RegistroResult.ServerError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.detail ?: "Error en el servidor.",
                        )
                    }
                }
                is RegistroResult.UnknownError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun validateInput(
        nombre: String,
        apellido: String,
        correo: String,
        contrasena: String,
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        val nombreTrimmed = nombre.trim()
        val apellidoTrimmed = apellido.trim()
        val correoTrimmed = correo.trim()

        if (nombreTrimmed.isEmpty()) {
            errors["nombre"] = "Este campo es obligatorio"
        } else if (nombreTrimmed.length > 100) {
            errors["nombre"] = "El nombre no puede exceder 100 caracteres"
        }

        if (apellidoTrimmed.isEmpty()) {
            errors["apellido"] = "Este campo es obligatorio"
        } else if (apellidoTrimmed.length > 100) {
            errors["apellido"] = "El apellido no puede exceder 100 caracteres"
        }

        if (correoTrimmed.isEmpty()) {
            errors["correo"] = "Este campo es obligatorio"
        } else if (correoTrimmed.length > 254) {
            errors["correo"] = "El correo no puede exceder 254 caracteres"
        } else if (!EMAIL_REGEX.matches(correoTrimmed)) {
            errors["correo"] = "Ingresa un correo electrónico válido"
        }

        if (contrasena.length !in 15..128) {
            errors["contrasena"] = "La contraseña debe tener entre 15 y 128 caracteres"
        }

        return errors
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    }
}

class RegistroViewModelFactory(
    private val repository: RegistroRepository = RegistroRepository(),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RegistroViewModel(repository) as T
    }
}
