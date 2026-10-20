package com.example.tickettrack.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.ui.theme.ModoTema
import com.example.tickettrack.viewmodel.TicketTrackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(viewModel: TicketTrackViewModel) {
    val context = LocalContext.current
    val usuario by viewModel.usuario.collectAsStateWithLifecycle()
    val horaRecordatorio by viewModel.horaRecordatorio.collectAsStateWithLifecycle()
    val modoTema by viewModel.modoTema.collectAsStateWithLifecycle()
    var dialogoRecordatorio by rememberSaveable { mutableStateOf(false) }
    var avisoNotificacion by rememberSaveable { mutableStateOf<String?>(null) }
    var probarAlPermitir by remember { mutableStateOf(false) }

    val permisoNotificacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            avisoNotificacion = null
            if (probarAlPermitir) viewModel.probarRecordatorio()
        } else {
            avisoNotificacion = "Activá las notificaciones para recibir el recordatorio."
        }
        probarAlPermitir = false
    }

    fun notificacionesPermitidas(): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !notificacionesPermitidas()) {
            permisoNotificacion.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Perfil", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = usuario?.email ?: "Sesión iniciada",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(28.dp))
        Text(text = "Apariencia", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ModoTema.entries.forEach { modo ->
            FilterChip(
                selected = modoTema == modo,
                onClick = { viewModel.cambiarModoTema(modo) },
                label = { Text(modo.etiqueta) },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Recordatorio", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Todos los días a las ${horaRecordatorio.texto()}: ¿Cargaste tus gastos de hoy?",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { dialogoRecordatorio = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cambiar hora")
        }

        if (avisoNotificacion != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = avisoNotificacion!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.salir() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (dialogoRecordatorio) {
        DialogoRecordatorio(
            hora = horaRecordatorio.hora,
            minuto = horaRecordatorio.minuto,
            onGuardar = { hora, minuto ->
                viewModel.cambiarHoraRecordatorio(hora, minuto)
                dialogoRecordatorio = false
            },
            onProbar = {
                if (notificacionesPermitidas()) {
                    avisoNotificacion = null
                    viewModel.probarRecordatorio()
                } else if (Build.VERSION.SDK_INT >= 33) {
                    probarAlPermitir = true
                    permisoNotificacion.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onCerrar = { dialogoRecordatorio = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoRecordatorio(
    hora: Int,
    minuto: Int,
    onGuardar: (Int, Int) -> Unit,
    onProbar: () -> Unit,
    onCerrar: () -> Unit
) {
    val estado = rememberTimePickerState(
        initialHour = hora,
        initialMinute = minuto,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Recordatorio diario") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Todos los días pregunta si cargaste los gastos.")
                Spacer(modifier = Modifier.height(12.dp))
                TimeInput(state = estado)
                TextButton(onClick = onProbar) { Text("Probar ahora") }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(estado.hour, estado.minute) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        }
    )
}
