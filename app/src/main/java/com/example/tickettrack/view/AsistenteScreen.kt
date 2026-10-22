package com.example.tickettrack.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Asistente",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Preguntá por tus gastos. Por ejemplo: ¿cuánto gasté en transporte este mes?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (alineadoAlUsuario) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (alineadoAlUsuario) 20.dp else 6.dp,
                            bottomEnd = if (alineadoAlUsuario) 6.dp else 20.dp
                        ),
                        color = if (alineadoAlUsuario) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text(
                            text = mensaje.texto,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (alineadoAlUsuario) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
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
