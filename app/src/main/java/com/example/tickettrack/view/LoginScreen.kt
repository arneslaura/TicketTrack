package com.example.tickettrack.view

import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.BuildConfig
import com.example.tickettrack.viewmodel.TicketTrackViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(viewModel: TicketTrackViewModel) {
    var creandoCuenta by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }

    if (creandoCuenta) {
        RegistroScreen(
            viewModel = viewModel,
            emailInicial = email,
            onEmailChange = { email = it },
            onVolver = {
                viewModel.limpiarErrorAuth()
                creandoCuenta = false
            }
        )
    } else {
        IngresoScreen(
            viewModel = viewModel,
            email = email,
            onEmailChange = { email = it },
            onCrearCuenta = {
                viewModel.limpiarErrorAuth()
                creandoCuenta = true
            }
        )
    }
}

@Composable
private fun IngresoScreen(
    viewModel: TicketTrackViewModel,
    email: String,
    onEmailChange: (String) -> Unit,
    onCrearCuenta: () -> Unit
) {
    val context = LocalContext.current
    var password by rememberSaveable { mutableStateOf("") }
    val errorAuth by viewModel.errorAuth.collectAsStateWithLifecycle()

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        try {
            val cuenta = GoogleSignIn.getSignedInAccountFromIntent(resultado.data)
                .getResult(ApiException::class.java)
            val token = cuenta.idToken
            if (token.isNullOrBlank()) {
                viewModel.mostrarErrorAuth("Google no devolvió un token. Revisá el ID de cliente web.")
            } else {
                viewModel.entrarConGoogle(token)
            }
        } catch (e: ApiException) {
            if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                viewModel.mostrarErrorAuth("No se pudo ingresar con Google. Probá de nuevo.")
            }
        }
    }

    FormularioAuth(titulo = "Ingresar") {
        CampoEmail(email = email, onEmailChange = onEmailChange)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        MensajeError(errorAuth)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val aviso = validarIngreso(email, password)
                if (aviso != null) {
                    viewModel.mostrarErrorAuth(aviso)
                } else {
                    viewModel.entrar(email.trim(), password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ingresar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                if (clientId.isBlank()) {
                    viewModel.mostrarErrorAuth(
                        "Falta activar Google en Firebase y pegar GOOGLE_WEB_CLIENT_ID en local.properties."
                    )
                    return@OutlinedButton
                }
                val opciones = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(clientId)
                    .requestEmail()
                    .build()
                googleLauncher.launch(GoogleSignIn.getClient(context, opciones).signInIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuar con Google")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onCrearCuenta, modifier = Modifier.fillMaxWidth()) {
            Text("Crear cuenta nueva")
        }
    }
}

@Composable
private fun RegistroScreen(
    viewModel: TicketTrackViewModel,
    emailInicial: String,
    onEmailChange: (String) -> Unit,
    onVolver: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf(emailInicial) }
    var password by rememberSaveable { mutableStateOf("") }
    var repetir by rememberSaveable { mutableStateOf("") }
    val errorAuth by viewModel.errorAuth.collectAsStateWithLifecycle()

    FormularioAuth(titulo = "Crear cuenta") {
        CampoEmail(
            email = email,
            onEmailChange = {
                email = it
                onEmailChange(it)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = { Text("Mínimo 6 caracteres") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = repetir,
            onValueChange = { repetir = it },
            label = { Text("Repetir contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        MensajeError(errorAuth)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val aviso = validarRegistro(email, password, repetir)
                if (aviso != null) {
                    viewModel.mostrarErrorAuth(aviso)
                } else {
                    viewModel.registrar(email.trim(), password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear cuenta")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Ya tengo cuenta")
        }
    }
}

@Composable
private fun FormularioAuth(
    titulo: String,
    contenido: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "TicketTrack",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = titulo, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(32.dp))
            contenido()
        }
    }
}

@Composable
private fun CampoEmail(email: String, onEmailChange: (String) -> Unit) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun MensajeError(error: String?) {
    if (error != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = error, color = MaterialTheme.colorScheme.error)
    }
}

private fun validarIngreso(email: String, password: String): String? {
    val correo = email.trim()
    if (correo.isBlank() || password.isBlank()) {
        return "Completá el email y la contraseña."
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
        return "El email no es válido."
    }
    return null
}

private fun validarRegistro(email: String, password: String, repetir: String): String? {
    val correo = email.trim()
    if (correo.isBlank() || password.isBlank() || repetir.isBlank()) {
        return "Completá el email y las dos contraseñas."
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
        return "El email no es válido."
    }
    if (password.length < 6) {
        return "La contraseña tiene que tener al menos 6 caracteres."
    }
    if (password != repetir) {
        return "Las contraseñas no coinciden."
    }
    return null
}
