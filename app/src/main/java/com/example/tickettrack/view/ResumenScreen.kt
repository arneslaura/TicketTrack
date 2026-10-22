package com.example.tickettrack.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.model.formatoPesos
import com.example.tickettrack.model.resumenDelMes
import com.example.tickettrack.model.textoComparacion
import com.example.tickettrack.viewmodel.TicketTrackViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ResumenScreen(
    viewModel: TicketTrackViewModel,
    onAbrirGasto: (String) -> Unit
) {
    val gastos by viewModel.gastos.collectAsStateWithLifecycle()
    var mesesAtras by rememberSaveable { mutableIntStateOf(0) }
    val calendario = Calendar.getInstance().apply { add(Calendar.MONTH, -mesesAtras) }
    val resumen = resumenDelMes(gastos, calendario)
    val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { mesesAtras += 1 }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Mes anterior"
                )
            }
            Text(
                text = resumen.titulo,
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(
                onClick = { if (mesesAtras > 0) mesesAtras -= 1 },
                enabled = mesesAtras > 0
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Mes siguiente"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                Text(
                    text = formatoPesos(resumen.total),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = if (resumen.cantidad == 1) "1 gasto" else "${resumen.cantidad} gastos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarjetaDato(
                titulo = "Promedio",
                valor = formatoPesos(resumen.promedio),
                modifier = Modifier.weight(1f)
            )
            TarjetaDato(
                titulo = "Mes anterior",
                valor = formatoPesos(resumen.totalMesAnterior),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = textoComparacion(resumen.diferenciaMesAnterior),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        resumen.mayor?.let { mayor ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Gasto más alto", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAbrirGasto(mayor.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = mayor.category.descripcion,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatoPesos(mayor.amount),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = formatoFecha.format(mayor.createdAt),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (resumen.porCategoria.isEmpty()) {
            Text(
                text = "Ese mes todavía no hay gastos.",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(text = "Por categoría", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            resumen.porCategoria.forEach { linea ->
                val porcentaje = (linea.proporcion * 100).roundToInt()
                val gastosTexto = if (linea.cantidad == 1) "1 gasto" else "${linea.cantidad} gastos"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = linea.categoria.descripcion,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatoPesos(linea.total),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$gastosTexto · $porcentaje%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { linea.proporcion },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TarjetaDato(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = titulo, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = valor, style = MaterialTheme.typography.titleLarge)
        }
    }
}
