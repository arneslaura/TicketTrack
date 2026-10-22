package com.example.tickettrack.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun MapaGasto(
    latitud: Double,
    longitud: Double,
    modifier: Modifier = Modifier,
    onMover: ((Double, Double) -> Unit)? = null
) {

    Column(modifier = modifier.fillMaxWidth()) {
    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(MapaBase.fuente)

                setMultiTouchControls(true)
                controller.setZoom(17.0)

                val puntoInicial = GeoPoint(latitud, longitud)
                controller.setCenter(puntoInicial)

                val marcador = Marker(this).apply {
                    position = puntoInicial
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    id = "marcador_gasto"
                }
                overlays.add(marcador)

                val receptorEventos = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        if (onMover != null) {
                            marcador.position = p
                            invalidate()
                            onMover(p.latitude, p.longitude)
                        }
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint): Boolean = false
                }
                overlays.add(MapEventsOverlay(receptorEventos))
            }
        },
        update = { mapView ->
            val puntoActualizado = GeoPoint(latitud, longitud)
            mapView.controller.animateTo(puntoActualizado)

            val marcador = mapView.overlays.filterIsInstance<Marker>().find { it.id == "marcador_gasto" }
            if (marcador != null) {
                marcador.position = puntoActualizado
                mapView.invalidate()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
        Text(
            text = MapaBase.ATRIBUCION,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}