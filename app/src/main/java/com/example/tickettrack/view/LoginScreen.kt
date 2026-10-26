package com.example.tickettrack.view

import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.BuildConfig
import com.example.tickettrack.R
import com.example.tickettrack.viewmodel.TicketTrackViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(viewModel: TicketTrackViewModel) {
    var creandoCuenta by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val guardando by viewModel.guardando.collectAsStateWithLifecycle()

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        try {
            val cuenta = GoogleSignIn.getSignedInAccountFromIntent(resultado.data)
                .getResult(ApiException::class.java)
            val token = cuenta.idToken
            if (token.isNullOrBlank()) {
                viewModel.mostrarErrorAuth(context.getString(R.string.error_google_no_token))
            } else {
                viewModel.entrarConGoogle(token)
            }
        } catch (e: ApiException) {
            if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                viewModel.mostrarErrorAuth(context.getString(R.string.error_google_fail))
            }
        }
    }

    val lanzarGoogle = {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            viewModel.mostrarErrorAuth(context.getString(R.string.error_google_setup))
        } else {
            val opciones = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build()
            googleLauncher.launch(GoogleSignIn.getClient(context, opciones).signInIntent)
        }
    }

    if (creandoCuenta) {
        RegistroScreen(
            viewModel = viewModel,
            emailInicial = email,
            guardando = guardando,
            onEmailChange = { email = it },
            onVolver = {
                viewModel.limpiarErrorAuth()
                creandoCuenta = false
            },
            onLanzarGoogle = lanzarGoogle
        )
    } else {
        IngresoScreen(
            viewModel = viewModel,
            email = email,
            guardando = guardando,
            onEmailChange = { email = it },
            onCrearCuenta = {
                viewModel.limpiarErrorAuth()
                creandoCuenta = true
            },
            onLanzarGoogle = lanzarGoogle
        )
    }
}

@Composable
private fun IngresoScreen(
    viewModel: TicketTrackViewModel,
    email: String,
    guardando: Boolean,
    onEmailChange: (String) -> Unit,
    onCrearCuenta: () -> Unit,
    onLanzarGoogle: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val errorAuth by viewModel.errorAuth.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    FormularioAuth(titulo = stringResource(R.string.login_title)) {
        CampoEmail(email = email, onEmailChange = onEmailChange, enabled = !guardando)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            enabled = !guardando,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        MensajeError(errorAuth)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                val avisoResId = validarIngreso(email, password)
                if (avisoResId != null) {
                    viewModel.mostrarErrorAuth(context.getString(avisoResId))
                } else {
                    viewModel.entrar(email.trim(), password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !guardando
        ) {
            if (guardando) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.btn_login))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        BotonGoogle(onClick = onLanzarGoogle, enabled = !guardando)

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onCrearCuenta, modifier = Modifier.fillMaxWidth(), enabled = !guardando) {
            Text(stringResource(R.string.link_create_account))
        }
    }
}

@Composable
private fun RegistroScreen(
    viewModel: TicketTrackViewModel,
    emailInicial: String,
    guardando: Boolean,
    onEmailChange: (String) -> Unit,
    onVolver: () -> Unit,
    onLanzarGoogle: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf(emailInicial) }
    var password by rememberSaveable { mutableStateOf("") }
    var repetir by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var repetirVisible by rememberSaveable { mutableStateOf(false) }

    val errorAuth by viewModel.errorAuth.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    FormularioAuth(titulo = stringResource(R.string.create_account_title)) {
        CampoEmail(
            email = email,
            enabled = !guardando,
            onEmailChange = {
                email = it
                onEmailChange(it)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            enabled = !guardando,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            supportingText = { Text(stringResource(R.string.password_min)) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = repetir,
            onValueChange = { repetir = it },
            label = { Text(stringResource(R.string.repeat_password_label)) },
            enabled = !guardando,
            visualTransformation = if (repetirVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            trailingIcon = {
                IconButton(onClick = { repetirVisible = !repetirVisible }) {
                    Icon(
                        imageVector = if (repetirVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        MensajeError(errorAuth)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                val avisoResId = validarRegistro(email, password, repetir)
                if (avisoResId != null) {
                    viewModel.mostrarErrorAuth(context.getString(avisoResId))
                } else {
                    viewModel.registrar(email.trim(), password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !guardando
        ) {
            if (guardando) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.btn_create_account))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        BotonGoogle(onClick = onLanzarGoogle, enabled = !guardando)

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth(), enabled = !guardando) {
            Text(stringResource(R.string.link_have_account))
        }
    }
}

@Composable
private fun BotonGoogle(onClick: () -> Unit, enabled: Boolean) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(stringResource(R.string.btn_continue_google))
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
            Spacer(modifier = Modifier.height(64.dp))

            val appName = buildAnnotatedString {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("Ticket")
                }
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)) {
                    append("Track")
                }
            }
            Text(
                text = appName,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.slogan),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(text = titulo, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(20.dp))
            contenido()
        }
    }
}

@Composable
private fun CampoEmail(email: String, onEmailChange: (String) -> Unit, enabled: Boolean) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(R.string.email_label)) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun MensajeError(error: String?) {
    if (error != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun validarIngreso(email: String, password: String): Int? {
    val correo = email.trim()
    if (correo.isBlank() || password.isBlank()) {
        return R.string.error_empty_login
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
        return R.string.error_invalid_email
    }
    return null
}

private fun validarRegistro(email: String, password: String, repetir: String): Int? {
    val correo = email.trim()
    if (correo.isBlank() || password.isBlank() || repetir.isBlank()) {
        return R.string.error_empty_register
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
        return R.string.error_invalid_email
    }
    if (password.length < 6) {
        return R.string.error_short_password
    }
    if (password != repetir) {
        return R.string.error_password_mismatch
    }
    return null
}
