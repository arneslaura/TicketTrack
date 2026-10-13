package com.example.tickettrack.data

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Suppress("DEPRECATION")
suspend fun etiquetaDeLugar(context: Context, latitud: Double, longitud: Double): String? {
    return withContext(Dispatchers.IO) {
        try {
            val direccion = Geocoder(context, Locale.getDefault())
                .getFromLocation(latitud, longitud, 1)
                ?.firstOrNull()
                ?: return@withContext null
            val nombre = direccion.featureName
            val ciudad = direccion.locality
            val linea = direccion.getAddressLine(0)
            when {
                !nombre.isNullOrBlank() && !ciudad.isNullOrBlank() && nombre != ciudad -> "$nombre, $ciudad"
                !linea.isNullOrBlank() -> linea
                !ciudad.isNullOrBlank() -> ciudad
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

fun textoUbicacion(placeLabel: String?, latitud: Double?, longitud: Double?): String? {
    if (!placeLabel.isNullOrBlank()) return placeLabel
    if (latitud == null || longitud == null) return null
    return String.format(Locale.US, "%.5f, %.5f", latitud, longitud)
}
