package com.example.tickettrack.view

import androidx.compose.foundation.gestures.snapping.SnapPosition.Center.position
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.BuildConfig
import com.example.tickettrack.model.formatoPesos
import com.example.tickettrack.viewmodel.TicketTrackViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.collections.map
import com.google.android.gms.maps.model.LatLng

private val BuenosAires = LatLng(-34.6037, -58.3816)

@Composable
fun MapaGastosScreen(
    viewModel: TicketTrackViewModel,
    onAbrirGasto: (String) -> Unit
) {
    val gastos by viewModel.gastos.collectAsStateWithLifecycle()
    val puntos = gastos.mapNotNull { gasto ->
        val latitud = gasto.latitude
        val longitud = gasto.longitude
        if (latitud == null || longitud == null) null
        else gasto to LatLng(latitud, longitud)
    }
    val sinUbicacion = gastos.size - puntos.size
    val marcadores = remember(puntos.map { it.first.id to it.second }) {
        puntos.map { (gasto, punto) -> gasto to MarkerState(position = punto) }
    }
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            puntos.firstOrNull()?.second ?: BuenosAires,
            if (puntos.size == 1) 15f else 11f
        )
    }

    LaunchedEffect(puntos.map { it.first.id }) {
        if (puntos.isEmpty()) {
            camara.position = CameraPosition.fromLatLngZoom(BuenosAires, 11f)
            return@LaunchedEffect
        }
        try {
            if (puntos.size == 1) {
                camara.move(CameraUpdateFactory.newLatLngZoom(puntos.first().second, 15f))
            } else {
                val limites = LatLngBounds.builder().apply {
                    puntos.forEach { include(it.second) }
                }.build()
                camara.move(CameraUpdateFactory.newLatLngBounds(limites, 80))
            }
        } catch (_: Exception) {
            camara.move(CameraUpdateFactory.newLatLngZoom(puntos.first().second, 12f))
        }
    }

    if (!BuildConfig.MAPS_ENABLED) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Para ver el mapa agregá MAPS_API_KEY en local.properties.",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camara,
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            marcadores.forEach { (gasto, estado) ->
                Marker(
                    state = estado,
                    title = "${gasto.category.descripcion} · ${formatoPesos(gasto.amount)}",
                    snippet = gasto.placeLabel,
                    onClick = {
                        onAbrirGasto(gasto.id)
                        true
                    }
                )
            }
        }

        val aviso = when {
            gastos.isEmpty() -> "Todavía no hay gastos. El mapa muestra Buenos Aires hasta que cargues uno con ubicación."
            puntos.isEmpty() -> "Ningún gasto tiene ubicación. Al cargarlo, tocá Usar mi ubicación."
            sinUbicacion > 0 -> "${puntos.size} en el mapa. $sinUbicacion sin ubicación."
            else -> "${puntos.size} en el mapa. Tocá un marcador para ver el gasto."
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 4.dp
        ) {
            Text(
                text = aviso,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
