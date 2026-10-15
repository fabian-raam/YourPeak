package com.frontendusuario.app.ui.registro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frontendusuario.app.R

@Composable
fun RegistroScreen(
    modifier: Modifier = Modifier,
    viewModel: RegistroViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Al completarse el registro con éxito, se limpia la contraseña en la memoria del Composable
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            contrasena = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.title_your_peak),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.subtitle_crear_cuenta),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Mensaje de éxito o información general
        if (uiState.isSuccess) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = uiState.successMessage
                        ?: stringResource(R.string.msg_cuenta_pendiente),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        // Mensaje de error general (conexión, tiempo de espera o servidor)
        uiState.generalError?.let { generalErrorMsg ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = generalErrorMsg,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        // Campo Nombre
        OutlinedTextField(
            value = nombre,
            onValueChange = { if (!uiState.isSuccess) nombre = it },
            label = { Text(stringResource(R.string.label_nombre)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && !uiState.isSuccess,
            isError = uiState.fieldErrors.containsKey("nombre"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
        )
        uiState.fieldErrors["nombre"]?.let { errorMsg ->
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Campo Apellido
        OutlinedTextField(
            value = apellido,
            onValueChange = { if (!uiState.isSuccess) apellido = it },
            label = { Text(stringResource(R.string.label_apellido)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && !uiState.isSuccess,
            isError = uiState.fieldErrors.containsKey("apellido"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
        )
        uiState.fieldErrors["apellido"]?.let { errorMsg ->
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Campo Correo Electrónico
        OutlinedTextField(
            value = correo,
            onValueChange = { if (!uiState.isSuccess) correo = it },
            label = { Text(stringResource(R.string.label_correo)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && !uiState.isSuccess,
            isError = uiState.fieldErrors.containsKey("correo"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )
        uiState.fieldErrors["correo"]?.let { errorMsg ->
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Campo Contraseña
        OutlinedTextField(
            value = contrasena,
            onValueChange = { if (!uiState.isSuccess) contrasena = it },
            label = { Text(stringResource(R.string.label_contrasena)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && !uiState.isSuccess,
            isError = uiState.fieldErrors.containsKey("contrasena"),
            singleLine = true,
            visualTransformation = if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(
                    onClick = { isPasswordVisible = !isPasswordVisible },
                    enabled = !uiState.isLoading && !uiState.isSuccess,
                ) {
                    Text(
                        text = stringResource(
                            if (isPasswordVisible) R.string.hide_password else R.string.show_password
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (!uiState.isLoading && !uiState.isSuccess) {
                        viewModel.registrar(nombre, apellido, correo, contrasena)
                    }
                },
            ),
        )
        uiState.fieldErrors["contrasena"]?.let { errorMsg ->
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón o Indicador de carga
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
            )
        } else {
            Button(
                onClick = {
                    viewModel.registrar(nombre, apellido, correo, contrasena)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isSuccess,
            ) {
                Text(
                    text = stringResource(R.string.btn_crear_cuenta),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
