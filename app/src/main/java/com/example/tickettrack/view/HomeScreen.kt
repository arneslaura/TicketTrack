package com.example.tickettrack.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.tickettrack.data.textoUbicacion
import com.example.tickettrack.model.CategoriaGasto
import com.example.tickettrack.model.Gasto
import com.example.tickettrack.model.PeriodoFiltro
import com.example.tickettrack.model.filtrarGastos
import com.example.tickettrack.model.formatoPesos
import com.example.tickettrack.viewmodel.TicketTrackViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TicketTrackViewModel,
    onAbrirGasto: (String) -> Unit
) {
    val gastos by viewModel.gastos.collectAsStateWithLifecycle()
    var categoriaNombre by rememberSaveable { mutableStateOf<String?>(null) }
    var periodoNombre by rememberSaveable { mutableStateOf(PeriodoFiltro.TODOS.name) }
    val periodo = PeriodoFiltro.valueOf(periodoNombre)
    val categoria = categoriaNombre?.let { nombre ->
        CategoriaGasto.entries.find { it.name == nombre }
    }
    val visibles = filtrarGastos(gastos, categoria, periodo)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TicketTrack",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Mis gastos",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodoFiltro.entries.forEach { opcion ->
                FilterChip(
                    selected = periodo == opcion,
                    onClick = { periodoNombre = opcion.name },
                    label = { Text(opcion.etiqueta) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = categoria == null,
                onClick = { categoriaNombre = null },
                label = { Text("Todas") }
            )
            CategoriaGasto.entries.forEach { opcion ->
                FilterChip(
                    selected = categoria == opcion,
                    onClick = { categoriaNombre = opcion.name },
                    label = { Text(opcion.descripcion) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

    
        if (visibles.isEmpty()) {
            Text(
                text = if (gastos.isEmpty()) {
                    "Todavía no hay gastos. Tocá + para cargar el primero."
                } else {
                    "Ningún gasto coincide con esos filtros."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibles, key = { it.id }) { gasto ->
                    ItemGasto(
                        gasto = gasto,
                        onClick = { onAbrirGasto(gasto.id) }
                    )
                }
            }
        }
            }
        }

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
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatoFecha.format(gasto.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                textoUbicacion(gasto.placeLabel, gasto.latitude, gasto.longitude)?.let { lugar ->
                    Text(
                        text = lugar,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                text = formatoPesos(gasto.amount),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
