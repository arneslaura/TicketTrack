package com.example.tickettrack.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapaGasto(
    latitud: Double,
    longitud: Double,
    modifier: Modifier = Modifier,
    onMover: ((LatLng) -> Unit)? = null
) {
    val punto = LatLng(latitud, longitud)
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(punto, 15f)
    }
    val marcador = remember { MarkerState(position = punto) }

    LaunchedEffect(latitud, longitud) {
        val nueva = LatLng(latitud, longitud)
        marcador.position = nueva
        camara.position = CameraPosition.fromLatLngZoom(nueva, camara.position.zoom)
    }

    GoogleMap(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        cameraPositionState = camara,
        uiSettings = MapUiSettings(zoomControlsEnabled = true),
        onMapClick = onMover
    ) {
        Marker(state = marcador)
    }
}
