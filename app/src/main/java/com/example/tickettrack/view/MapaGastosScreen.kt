package com.example.tickettrack.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.model.formatoPesos
import com.example.tickettrack.ui.LocalIdioma
import com.example.tickettrack.ui.en
import com.example.tickettrack.ui.texto
import com.example.tickettrack.viewmodel.TicketTrackViewModel
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private val BuenosAires = GeoPoint(-34.6037, -58.3816)

@Composable
fun MapaGastosScreen(
    viewModel: TicketTrackViewModel,
    onAbrirGasto: (String) -> Unit
) {
    val context = LocalContext.current
    val idioma = LocalIdioma.current
    val gastos by viewModel.gastos.collectAsStateWithLifecycle()
    val puntos = gastos.mapNotNull { gasto ->
        val latitud = gasto.latitude
        val longitud = gasto.longitude
        if (
            latitud == null ||
            longitud == null ||
            latitud !in -90.0..90.0 ||
            longitud !in -180.0..180.0
        ) {
            null
        } else {
            gasto to GeoPoint(latitud, longitud)
        }
    }
    val clavePuntos = puntos.joinToString("|") { "${it.first.id}:${it.second.latitude}:${it.second.longitude}" }
    val sinUbicacion = gastos.size - puntos.size

    val mapa = remember {
        MapView(context).apply {
            setTileSource(MapaBase.fuente)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            controller.setZoom(12.0)
            controller.setCenter(BuenosAires)
        }
    }

    DisposableEffect(mapa) {
        mapa.onResume()
        onDispose {
            mapa.onPause()
            runCatching { mapa.onDetach() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapa },
            update = { vista ->
                if (vista.tag == clavePuntos) return@AndroidView
                vista.tag = clavePuntos
                vista.overlays.removeAll { it is Marker }
                puntos.forEach { (gasto, punto) ->
                    val marcador = Marker(vista).apply {
                        position = punto
                        title = "${gasto.category.en(idioma)} · ${formatoPesos(gasto.amount)}"
                        snippet = gasto.placeLabel
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            onAbrirGasto(gasto.id)
                            true
                        }
                    }
                    vista.overlays.add(marcador)
                }
                vista.post {
                    if (vista.width <= 0 || vista.height <= 0) return@post
                    try {
                        when (puntos.size) {
                            0 -> {
                                vista.controller.setZoom(12.0)
                                vista.controller.setCenter(BuenosAires)
                            }
                            1 -> {
                                vista.controller.setZoom(16.0)
                                vista.controller.setCenter(puntos.first().second)
                            }
                            else -> {
                                val caja = BoundingBox.fromGeoPoints(puntos.map { it.second })
                                vista.zoomToBoundingBox(caja, false, 80)
                            }
                        }
                    } catch (_: Exception) {
                        vista.controller.setZoom(12.0)
                        vista.controller.setCenter(puntos.firstOrNull()?.second ?: BuenosAires)
                    }
                    vista.invalidate()
                }
            }
        )

        val aviso = when {
            gastos.isEmpty() -> texto(
                idioma,
                "Todavía no hay gastos. El mapa muestra Buenos Aires hasta que cargues uno con ubicación.",
                "No expenses yet. The map shows Buenos Aires until you add one with a location.",
                "Ainda não há gastos. O mapa mostra Buenos Aires até você adicionar um com localização."
            )
            puntos.isEmpty() -> texto(
                idioma,
                "Ningún gasto tiene ubicación. Al cargarlo, tocá Usar mi ubicación.",
                "No expense has a location yet. When you add one, tap Use my location.",
                "Nenhum gasto tem localização. Ao adicionar, toque em Usar minha localização."
            )
            sinUbicacion > 0 -> texto(
                idioma,
                "${puntos.size} en el mapa. $sinUbicacion sin ubicación.",
                "${puntos.size} on the map. $sinUbicacion without a location.",
                "${puntos.size} no mapa. $sinUbicacion sem localização."
            )
            else -> texto(
                idioma,
                "${puntos.size} en el mapa. Tocá un marcador para ver el gasto.",
                "${puntos.size} on the map. Tap a marker to open the expense.",
                "${puntos.size} no mapa. Toque um marcador para ver o gasto."
            )
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
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = aviso,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = MapaBase.ATRIBUCION,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
