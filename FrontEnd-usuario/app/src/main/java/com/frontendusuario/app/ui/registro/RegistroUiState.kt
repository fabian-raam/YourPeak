package com.frontendusuario.app.ui.registro

/**
 * Estado de la interfaz para la pantalla de registro.
 * IMPORTANTE: No incluye la contraseña para proteger la privacidad.
 */
data class RegistroUiState(
    val isLoading: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val generalError: String? = null,
    val isSuccess: Boolean = false,
    val successMessage: String? = null,
)
