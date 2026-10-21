
package com.example.tickettrack.view

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.model.formatoPesos
import com.example.tickettrack.viewmodel.TicketTrackViewModel
import org.osmdroid.config.Configuration
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

    // Configuración de OSMDroid.
    Configuration.getInstance().apply {
        userAgentValue = "TicketTrack_UNAJ_Laura_1.0"
        load(
            context,
            context.getSharedPreferences(
                "osmdroid",
                Context.MODE_PRIVATE
            )
        )
    }

    // Solo se incluyen gastos con coordenadas válidas.
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

    val sinUbicacion = gastos.size - puntos.size

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),

            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    controller.setZoom(12.0)
                    controller.setCenter(BuenosAires)
                }
            },

            update = { mapa ->
                // Evita duplicar marcadores cuando Compose actualiza la vista.
                mapa.overlays.removeAll { it is Marker }

                puntos.forEach { (gasto, punto) ->
                    val marcador = Marker(mapa).apply {
                        position = punto

                        title = "${gasto.category.descripcion} · ${
                            formatoPesos(gasto.amount)
                        }"

                        snippet = gasto.placeLabel

                        setAnchor(
                            Marker.ANCHOR_CENTER,
                            Marker.ANCHOR_BOTTOM
                        )

                        setOnMarkerClickListener { _, _ ->
                            onAbrirGasto(gasto.id)
                            true
                        }
                    }

                    mapa.overlays.add(marcador)
                }

                // Ajusta la cámara según la cantidad de gastos.
                when (puntos.size) {
                    0 -> {
                        mapa.controller.setZoom(12.0)
                        mapa.controller.setCenter(BuenosAires)
                    }

                    1 -> {
                        mapa.controller.setZoom(16.0)
                        mapa.controller.setCenter(puntos.first().second)
                    }

                    else -> {
                        val caja = BoundingBox.fromGeoPoints(
                            puntos.map { it.second }
                        )

                        mapa.zoomToBoundingBox(caja, true, 80)
                    }
                }

                mapa.invalidate()
            }
        )

        val aviso = when {
            gastos.isEmpty() ->
                "Todavía no hay gastos. El mapa muestra Buenos Aires hasta que cargues uno con ubicación."

            puntos.isEmpty() ->
                "Ningún gasto tiene ubicación. Al cargarlo, tocá Usar mi ubicación."

            sinUbicacion > 0 ->
                "${puntos.size} en el mapa. $sinUbicacion sin ubicación."

            else ->
                "${puntos.size} en el mapa. Tocá un marcador para ver el gasto."
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
