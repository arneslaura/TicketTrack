package com.example.tickettrack.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.*
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
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.data.textoUbicacion
import com.example.tickettrack.model.Gasto
import com.example.tickettrack.viewmodel.TicketTrackViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: TicketTrackViewModel,
    onNavegarAddGasto: () -> Unit,
    onAbrirGasto: (String) -> Unit,
    onAbrirResumen: () -> Unit
) {
    //obtener lista de gastos del view model
    val context = LocalContext.current
    val gastos by viewModel.gastos.collectAsStateWithLifecycle()
    val horaRecordatorio by viewModel.horaRecordatorio.collectAsStateWithLifecycle()
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
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        viewModel.obtenerGastos()
        if (Build.VERSION.SDK_INT >= 33 && !notificacionesPermitidas()) {
            permisoNotificacion.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavegarAddGasto() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TicketTrack",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(onClick = { viewModel.salir() }) {
                    Text("Salir")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Mis Gastos",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedButton(
                onClick = onAbrirResumen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Resumen del mes")
            }

            if (avisoNotificacion != null) {
                Text(
                    text = avisoNotificacion!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            //lista dinamica
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gastos) { gasto ->
                    ItemGasto(
                        gasto = gasto,
                        onClick = { onAbrirGasto(gasto.id) }
                    )
                }
            }
        }
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
                TextButton(onClick = onProbar) {
                    Text("Probar ahora")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(estado.hour, estado.minute) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text("Cerrar")
            }
        }
    )
}

// diseño individual de cada tarjeta de gasto
@Composable
fun ItemGasto(gasto: Gasto, onClick: () -> Unit) {
    val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!gasto.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = gasto.photoUrl,
                    contentDescription = "Miniatura del ticket",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gasto.category.descripcion,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary // Usa tu verde
                )
                Text(
                    text = formatoFecha.format(gasto.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                textoUbicacion(gasto.placeLabel, gasto.latitude, gasto.longitude)?.let { lugar ->
                    Text(
                        text = lugar,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Text(
                text = "$${gasto.amount}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}