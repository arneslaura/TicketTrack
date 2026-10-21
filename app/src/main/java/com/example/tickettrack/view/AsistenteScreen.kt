package com.example.tickettrack.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tickettrack.viewmodel.TicketTrackViewModel

private data class MensajeChat(val delUsuario: Boolean, val texto: String)

@Composable
fun AsistenteScreen(viewModel: TicketTrackViewModel) {
    val mensajes = remember { mutableStateListOf<MensajeChat>() }
    var pregunta by remember { mutableStateOf("") }
    var pensando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val lista = rememberLazyListState()

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) lista.animateScrollToItem(mensajes.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Asistente",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Preguntá por tus gastos. Por ejemplo: ¿cuánto gasté en transporte este mes?",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = lista,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (mensajes.isEmpty()) {
                item {
                    Text(
                        text = "Todavía no hay preguntas.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            items(mensajes) { mensaje ->
                val alineadoAlUsuario = mensaje.delUsuario
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (alineadoAlUsuario) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Text(
                        text = mensaje.texto,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = pregunta,
                onValueChange = { pregunta = it },
                modifier = Modifier.weight(1f),
                label = { Text("Tu pregunta") },
                enabled = !pensando,
                maxLines = 3
            )
            Button(
                onClick = {
                    val texto = pregunta.trim()
                    if (texto.isEmpty() || pensando) return@Button
                    pregunta = ""
                    error = null
                    mensajes.add(MensajeChat(delUsuario = true, texto = texto))
                    pensando = true
                    val historial = mensajes.map { mensaje ->
                        (if (mensaje.delUsuario) "user" else "model") to mensaje.texto
                    }
                    viewModel.preguntar(
                        historial = historial,
                        onListo = { respuesta ->
                            mensajes.add(MensajeChat(delUsuario = false, texto = respuesta))
                            pensando = false
                        },
                        onError = { mensaje ->
                            if (mensajes.isNotEmpty() && mensajes.last().delUsuario) {
                                mensajes.removeAt(mensajes.lastIndex)
                            }
                            pregunta = texto
                            error = mensaje
                            pensando = false
                        }
                    )
                },
                enabled = pregunta.isNotBlank() && !pensando,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(if (pensando) "..." else "Enviar")
            }
        }
    }
}
