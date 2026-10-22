package com.example.tickettrack.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.tickettrack.data.textoUbicacion
import com.example.tickettrack.model.CategoriaGasto
import com.example.tickettrack.model.Gasto
import com.example.tickettrack.model.PeriodoFiltro
import com.example.tickettrack.model.filtrarGastos
import com.example.tickettrack.model.formatoPesos
import com.example.tickettrack.ui.LocalIdioma
import com.example.tickettrack.ui.en
import com.example.tickettrack.ui.texto
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
    val enLinea by viewModel.enLinea.collectAsStateWithLifecycle()
    val idioma = LocalIdioma.current
    var categoriaNombre by rememberSaveable { mutableStateOf<String?>(null) }
    var periodoNombre by rememberSaveable { mutableStateOf(PeriodoFiltro.TODOS.name) }
    val periodo = PeriodoFiltro.valueOf(periodoNombre)
    var menuCategoria by rememberSaveable { mutableStateOf(false) }
    val categoria = categoriaNombre?.let { nombre ->
        CategoriaGasto.entries.find { it.name == nombre }
    }
    val visibles = filtrarGastos(gastos, categoria, periodo)
    val totalVisible = visibles.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = texto(idioma, "Mis gastos", "My expenses", "Minhas despesas"),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                Text(
                    text = periodo.en(idioma),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatoPesos(totalVisible),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = when (visibles.size) {
                        0 -> texto(idioma, "Sin gastos en este filtro", "No expenses in this filter", "Sem gastos neste filtro")
                        1 -> texto(idioma, "1 gasto", "1 expense", "1 gasto")
                        else -> texto(
                            idioma,
                            "${visibles.size} gastos",
                            "${visibles.size} expenses",
                            "${visibles.size} gastos"
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                )
            }
        }

        if (!enLinea) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = texto(
                        idioma,
                        "Sin conexión. Estás viendo los gastos guardados en el teléfono.",
                        "You are offline. Showing the expenses saved on this phone.",
                        "Sem conexão. Mostrando os gastos salvos no telefone."
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                PeriodoFiltro.entries.forEach { opcion ->
                    val elegido = periodo == opcion
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (elegido) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { periodoNombre = opcion.name }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opcion.en(idioma),
                            color = if (elegido) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = menuCategoria,
            onExpandedChange = { menuCategoria = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val elegida = categoria
                    if (elegida == null) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = iconoCategoria(elegida),
                            contentDescription = null,
                            tint = colorCategoria(elegida)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = texto(idioma, "Categoría", "Category", "Categoria"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = elegida?.en(idioma) ?: texto(idioma, "Todas", "All", "Todas"),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            ExposedDropdownMenu(
                expanded = menuCategoria,
                onDismissRequest = { menuCategoria = false }
            ) {
                DropdownMenuItem(
                    text = { Text(texto(idioma, "Todas", "All", "Todas")) },
                    leadingIcon = {
                        Icon(Icons.Filled.GridView, contentDescription = null)
                    },
                    onClick = {
                        categoriaNombre = null
                        menuCategoria = false
                    }
                )
                CategoriaGasto.entries.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion.en(idioma)) },
                        leadingIcon = {
                            Icon(
                                imageVector = iconoCategoria(opcion),
                                contentDescription = null,
                                tint = colorCategoria(opcion)
                            )
                        },
                        onClick = {
                            categoriaNombre = opcion.name
                            menuCategoria = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

    
        if (visibles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (gastos.isEmpty()) {
                        texto(
                            idioma,
                            "Todavía no hay gastos.\nTocá + para cargar el primero.",
                            "No expenses yet.\nTap + to add the first one.",
                            "Ainda não há gastos.\nToque em + para adicionar o primeiro."
                        )
                    } else {
                        texto(
                            idioma,
                            "Ningún gasto coincide con esos filtros.",
                            "No expenses match those filters.",
                            "Nenhum gasto corresponde a esses filtros."
                        )
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)
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
    val idioma = LocalIdioma.current
    val formatoFecha = SimpleDateFormat("d MMM", Locale.getDefault())
    val titulo = gasto.description.ifBlank { gasto.category.en(idioma) }
    val detalle = buildString {
        append(gasto.category.en(idioma))
        append(" · ")
        append(formatoFecha.format(gasto.createdAt))
        textoUbicacion(gasto.placeLabel, gasto.latitude, gasto.longitude)?.let {
            append(" · ")
            append(it)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!gasto.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = gasto.photoUrl,
                    contentDescription = "Miniatura del ticket",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colorCategoria(gasto.category).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconoCategoria(gasto.category),
                        contentDescription = null,
                        tint = colorCategoria(gasto.category),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatoPesos(gasto.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

