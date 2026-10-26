package com.example.tickettrack.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.R
import com.example.tickettrack.model.CategoriaGasto
import com.example.tickettrack.model.formatoPesos
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
                    val tituloCategoria = when (gasto.category) {
                        CategoriaGasto.SUPERMERCADO -> context.getString(R.string.cat_supermarket)
                        CategoriaGasto.TRANSPORTE -> context.getString(R.string.cat_transport)
                        CategoriaGasto.SERVICIOS -> context.getString(R.string.cat_services)
                        CategoriaGasto.OCIO -> context.getString(R.string.cat_leisure)
                        CategoriaGasto.SALUD -> context.getString(R.string.cat_health)
                        CategoriaGasto.OTROS -> context.getString(R.string.cat_others)
                    }

                    val marcador = Marker(vista).apply {
                        position = punto
                        title = "$tituloCategoria · ${formatoPesos(gasto.amount)}"
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
            gastos.isEmpty() -> stringResource(R.string.map_empty_expenses)
            puntos.isEmpty() -> stringResource(R.string.map_no_locations)
            else -> stringResource(R.string.map_tap_marker)
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.9f)
                .padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = aviso,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = MapaBase.ATRIBUCION,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}