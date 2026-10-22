package com.example.tickettrack.data

import android.content.Context
import com.example.tickettrack.model.CategoriaGasto
import com.example.tickettrack.model.Gasto
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

object CacheGastos {
    private const val CARPETA = "tickettrack_offline"

    fun leer(context: Context, userId: String): List<Gasto> {
        val archivo = archivoDe(context, userId)
        if (!archivo.exists()) return emptyList()
        return try {
            val lista = JSONArray(archivo.readText())
            buildList {
                for (i in 0 until lista.length()) {
                    val item = lista.getJSONObject(i)
                    val categoria = CategoriaGasto.entries.find {
                        it.name == item.optString("category")
                    } ?: CategoriaGasto.OTROS
                    add(
                        Gasto(
                            id = item.optString("id"),
                            userId = item.optString("userId"),
                            amount = item.optDouble("amount"),
                            category = categoria,
                            description = item.optString("description"),
                            photoUrl = item.optString("photoUrl").ifBlank { null },
                            latitude = item.optNullableDouble("latitude"),
                            longitude = item.optNullableDouble("longitude"),
                            placeLabel = item.optString("placeLabel").ifBlank { null },
                            createdAt = Date(item.optLong("createdAt")),
                            updatedAt = Date(item.optLong("updatedAt"))
                        )
                    )
                }
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun guardar(context: Context, userId: String, gastos: List<Gasto>) {
        val lista = JSONArray()
        gastos.forEach { gasto ->
            lista.put(
                JSONObject()
                    .put("id", gasto.id)
                    .put("userId", gasto.userId)
                    .put("amount", gasto.amount)
                    .put("category", gasto.category.name)
                    .put("description", gasto.description)
                    .put("photoUrl", gasto.photoUrl ?: "")
                    .put("latitude", gasto.latitude ?: JSONObject.NULL)
                    .put("longitude", gasto.longitude ?: JSONObject.NULL)
                    .put("placeLabel", gasto.placeLabel ?: "")
                    .put("createdAt", gasto.createdAt.time)
                    .put("updatedAt", gasto.updatedAt.time)
            )
        }
        val archivo = archivoDe(context, userId)
        archivo.parentFile?.mkdirs()
        archivo.writeText(lista.toString())
    }

    private fun archivoDe(context: Context, userId: String) =
        context.filesDir.resolve("$CARPETA/$userId.json")

    private fun JSONObject.optNullableDouble(clave: String): Double? {
        if (!has(clave) || isNull(clave)) return null
        return optDouble(clave).takeIf { !it.isNaN() }
    }
}
