package com.ramirez.yourpeak.ui.auth

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.ramirez.yourpeak.R
import com.ramirez.yourpeak.data.model.GoogleChallenge
import com.ramirez.yourpeak.data.model.GoogleCompleteRequest
import com.ramirez.yourpeak.data.model.LoginResponse
import com.ramirez.yourpeak.domain.Fechas
import com.ramirez.yourpeak.ui.components.Aviso
import com.ramirez.yourpeak.ui.components.TipoAviso
import com.ramirez.yourpeak.ui.theme.CampoBorde
import com.ramirez.yourpeak.ui.theme.TextoSuave
import kotlinx.coroutines.launch

@Composable
internal fun GoogleAccess(
    label: String,
    manualBusy: Boolean,
    onAuthenticated: (LoginResponse) -> Unit,
    content: @Composable (Boolean, @Composable () -> Unit) -> Unit
) {
    val viewModel: GoogleAccessViewModel = viewModel(factory = GoogleAccessViewModel.Factory)
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.user) {
        state.user?.let(onAuthenticated)
    }

    content(state.busy || state.pending != null) {
        GoogleButton(if (state.busy && state.pending == null) "Conectando con Google…" else label, !manualBusy && !state.busy) {
            focus.clearFocus()
            scope.launch {
                viewModel.start { challenge ->
                    try {
                        selectGoogleAccount(context, challenge)
                    } catch (_: GetCredentialCancellationException) {
                        null
                    } catch (_: GetCredentialException) {
                        viewModel.credentialError("No se pudo abrir Google. Inténtalo de nuevo.")
                        null
                    } catch (_: GoogleIdTokenParsingException) {
                        viewModel.credentialError("Google devolvió una credencial inválida.")
                        null
                    }
                }
            }
        }
        if (state.pending == null) Aviso(state.message, TipoAviso.Error)
    }

    state.pending?.let { pending ->
        key(pending.flowId) {
            GooglePendingDialog(pending, state.busy, state.message, viewModel::dismiss, viewModel::complete)
        }
    }
}

private suspend fun selectGoogleAccount(context: Context, challenge: GoogleChallenge): String {
    if (challenge.clientId.isNullOrBlank() || challenge.nonce.isNullOrBlank()) {
        throw GoogleIdTokenParsingException()
    }
    val option = GetSignInWithGoogleOption.Builder(challenge.clientId).setNonce(challenge.nonce).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = CredentialManager.create(context).getCredential(context, request).credential
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
    throw GoogleIdTokenParsingException()
}

@Composable
internal fun GoogleButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF8E918F)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF131314), contentColor = Color.White)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun GoogleDivider() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(Modifier.weight(1f), color = CampoBorde)
        Text("o", color = TextoSuave, fontSize = 13.sp)
        HorizontalDivider(Modifier.weight(1f), color = CampoBorde)
    }
}

@Composable
private fun GooglePendingDialog(
    pending: GooglePending,
    busy: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onComplete: (GoogleCompleteRequest) -> Unit
) {
    var name by remember { mutableStateOf(pending.profile.nombre) }
    var lastName by remember { mutableStateOf(pending.profile.apellido) }
    var birthDate by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    val canSubmit = !busy && if (pending.link) password.isNotBlank()
        else name.isNotBlank() && lastName.isNotBlank() && birthDate.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (pending.link) "Vincula tu cuenta" else "Completa tu registro") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pending.profile.correo)
                if (pending.link) {
                    CampoAutenticacion(
                        valor = password, alCambiar = { password = it }, etiqueta = "Contraseña de YourPeak",
                        icono = R.drawable.ic_lock, placeholder = "Ingresa tu contraseña",
                        esContrasena = true, contrasenaVisible = showPassword,
                        alCambiarVisibilidad = { showPassword = !showPassword }, habilitado = !busy,
                        longitudMaxima = 128,
                        alEnviar = {
                            if (canSubmit) onComplete(GoogleCompleteRequest(pending.flowId, contrasena = password))
                        }
                    )
                } else {
                    CampoAutenticacion(
                        valor = name, alCambiar = { name = it }, etiqueta = "Nombre",
                        icono = null, placeholder = "Tu nombre", habilitado = !busy, longitudMaxima = 100
                    )
                    CampoAutenticacion(
                        valor = lastName, alCambiar = { lastName = it }, etiqueta = "Apellido",
                        icono = null, placeholder = "Tu apellido", habilitado = !busy, longitudMaxima = 100
                    )
                    CampoAutenticacion(
                        valor = Fechas.paraMostrar(birthDate), alCambiar = {}, etiqueta = "Fecha de nacimiento",
                        icono = R.drawable.ic_calendar, placeholder = "dd/mm/aaaa",
                        habilitado = !busy, longitudMaxima = 10, soloLectura = true, alPresionar = { showCalendar = true }
                    )
                }
                Aviso(message, TipoAviso.Error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = {
                    val request = if (pending.link) GoogleCompleteRequest(pending.flowId, contrasena = password)
                    else GoogleCompleteRequest(pending.flowId, name.trim(), lastName.trim(), birthDate)
                    onComplete(request)
                }
            ) { Text(if (busy) "Un momento…" else "Continuar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } }
    )
    if (showCalendar) {
        SelectorFecha(birthDate, { birthDate = it; showCalendar = false }, { showCalendar = false })
    }
}
