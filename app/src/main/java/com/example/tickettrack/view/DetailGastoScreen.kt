package com.example.tickettrack.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.tickettrack.BuildConfig
import com.example.tickettrack.data.textoUbicacion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.viewmodel.TicketTrackViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DetailGastoScreen(
    viewModel: TicketTrackViewModel,
    gastoId: String,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onEliminado: () -> Unit
) {
    // La lista ya vive en el ViewModel. Acá solo buscamos el gasto que el usuario tocó.
    val gastos by viewModel.gastos.collectAsStateWithLifecycle()
    val gasto = gastos.find { it.id == gastoId }
    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
    var confirmarBorrado by rememberSaveable { mutableStateOf(false) }
    var fotoAmpliada by rememberSaveable { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onVolver) {
                Text(
                    text = "← Volver",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (gasto == null) {
                Text(
                    text = "No encontramos ese gasto.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = gasto.category.descripcion,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$${gasto.amount}",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatoFecha.format(gasto.createdAt),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Nota",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = gasto.description.ifBlank { "Sin nota" },
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Comprobante",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (gasto.photoUrl.isNullOrBlank()) {
                    Text(
                        text = "Sin comprobante",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    AsyncImage(
                        model = gasto.photoUrl,
                        contentDescription = "Foto del ticket",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { fotoAmpliada = true },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tocá la foto para ampliarla",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Lugar",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                val textoLugar = textoUbicacion(gasto.placeLabel, gasto.latitude, gasto.longitude)
                if (textoLugar == null || gasto.latitude == null || gasto.longitude == null) {
                    Text(
                        text = "Sin ubicación",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        text = textoLugar,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (BuildConfig.MAPS_ENABLED) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MapaGasto(
                            latitud = gasto.latitude,
                            longitud = gasto.longitude
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onEditar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Editar")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { confirmarBorrado = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            }
        }
    }

    if (fotoAmpliada && !gasto?.photoUrl.isNullOrBlank()) {
        Dialog(onDismissRequest = { fotoAmpliada = false }) {
            AsyncImage(
                model = gasto?.photoUrl,
                contentDescription = "Ticket ampliado",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("Eliminar gasto") },
            text = { Text("Se va a borrar este gasto de tu listado.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarBorrado = false
                        viewModel.eliminarGasto(gastoId, onEliminado)
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
