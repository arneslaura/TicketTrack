package com.example.tickettrack.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.model.Gasto
import com.example.tickettrack.viewmodel.TicketTrackViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: TicketTrackViewModel,
    onNavegarAddGasto: () -> Unit
) {
    //obtener lista de gastos del view model
    val gastos by viewModel.gastos.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.obtenerGastos()
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
                    text = buildAnnotatedString {
                        append("Ticket")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Track")
                        }
                    },
                    style = MaterialTheme.typography.headlineLarge
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

            Spacer(modifier = Modifier.height(8.dp))

            //lista dinamica
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gastos) { gasto ->
                    ItemGasto(gasto = gasto)
                }
            }
        }
    }
}

// diseño individual de cada tarjeta de gasto
@Composable
fun ItemGasto(gasto: Gasto) {
    val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Column {
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
            }
            Text(
                text = "$${gasto.amount}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}