package com.example.tickettrack.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

suspend fun etiquetaDeLugar(latitud: Double, longitud: Double): String? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://nominatim.openstreetmap.org/reverse" +
                    "?lat=$latitud&lon=$longitud&format=json&accept-language=es"
            )
            val conexion = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "TicketTrack/1.0 (com.example.tickettrack; Android)"
                )
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conexion.responseCode !in 200..299) return@withContext null
            val cuerpo = conexion.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(cuerpo)
            val direccion = json.optJSONObject("address")
            val calle = direccion.primerTexto("road", "pedestrian", "neighbourhood")
            val ciudad = direccion.primerTexto("city", "town", "village", "suburb", "state")
            when {
                calle.isNotBlank() && ciudad.isNotBlank() -> "$calle, $ciudad"
                ciudad.isNotBlank() -> ciudad
                else -> json.optString("display_name")
                    .substringBefore(",")
                    .takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }
}

private fun JSONObject?.primerTexto(vararg claves: String): String {
    if (this == null) return ""
    return claves.firstNotNullOfOrNull { clave ->
        optString(clave).takeIf { it.isNotBlank() }
    }.orEmpty()
}

fun textoUbicacion(placeLabel: String?, latitud: Double?, longitud: Double?): String? {
    if (!placeLabel.isNullOrBlank()) return placeLabel
    if (latitud == null || longitud == null) return null
    return String.format(Locale.US, "%.5f, %.5f", latitud, longitud)
}
