package com.example.tickettrack.data

import android.util.Base64
import com.example.tickettrack.BuildConfig
import com.example.tickettrack.model.CategoriaGasto
import com.example.tickettrack.model.Gasto
import com.example.tickettrack.model.SugerenciaGasto
import com.example.tickettrack.model.formatoPesos
import com.example.tickettrack.model.resumenDelMes
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

object GeminiCliente {

    private const val MODELO = "gemini-3.8-flash"

    private const val INSTRUCCION_GASTO =
        "Sos un asistente de gastos personales en Argentina. " +
                "Respondé solo con JSON, sin markdown, con las claves " +
                "amount (número con punto decimal, o null), " +
                "category (una de: SUPERMERCADO, TRANSPORTE, SERVICIOS, OCIO, SALUD, OTROS), " +
                "description (nota corta en español) y place (lugar o null)."

    fun leerTicket(jpeg: ByteArray): SugerenciaGasto {
        val partes = JSONArray()
            .put(JSONObject().put("text", "Leé este ticket y completá el gasto."))
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP))
                )
            )
        val texto = generar(instruccion = INSTRUCCION_GASTO, partes = partes, json = true)
        return sugerenciaDesde(texto)
    }

    fun interpretarFrase(frase: String): SugerenciaGasto {
        val partes = JSONArray().put(
            JSONObject().put("text", "El usuario dijo: \"$frase\"")
        )
        val texto = generar(instruccion = INSTRUCCION_GASTO, partes = partes, json = true)
        return sugerenciaDesde(texto)
    }

    fun preguntar(gastos: List<Gasto>, historial: List<Pair<String, String>>): String {
        val contenidos = JSONArray()
        historial.forEach { (rol, texto) ->
            contenidos.put(
                JSONObject()
                    .put("role", rol)
                    .put("parts", JSONArray().put(JSONObject().put("text", texto)))
            )
        }
        val instruccion = "Respondé en español, de forma amigable y concisa, sobre los gastos de TicketTrack. " +
                "Usá solo los datos del contexto. Si no tenés la información, decilo amablemente.\n\n" +
                contextoDe(gastos)
        return generar(instruccion = instruccion, contenidos = contenidos, json = false)
    }

    private fun contextoDe(gastos: List<Gasto>): String {
        val resumen = resumenDelMes(gastos)
        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR"))

        val porCategoria = resumen.porCategoria.joinToString("\n") {
            "- ${it.categoria.descripcion}: ${formatoPesos(it.total)}"
        }

        val recientes = gastos.take(40).joinToString("\n") { gasto ->
            val lugar = gasto.placeLabel?.takeIf { it.isNotBlank() } ?: ""
            "${fecha.format(gasto.createdAt)} | ${gasto.category.descripcion} | " +
                    "${formatoPesos(gasto.amount)} | ${gasto.description} | $lugar"
        }

        return "Mes ${resumen.titulo}: total ${formatoPesos(resumen.total)}.\n" +
                "Gastos por categoría:\n" +
                porCategoria.ifBlank { "Sin gastos en el mes." } +
                "\n\nGastos recientes:\n" +
                recientes.ifBlank { "No hay gastos cargados." }
    }

    private fun generar(
        instruccion: String,
        partes: JSONArray? = null,
        contenidos: JSONArray? = null,
        json: Boolean
    ): String {
        val clave = BuildConfig.GEMINI_API_KEY
        if (clave.isBlank()) {
            throw IllegalStateException(
                "Falta GEMINI_API_KEY en local.properties. Creala en aistudio.google.com/apikey."
            )
        }
        val cuerpo = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruccion)))
            )
            .put(
                "contents",
                contenidos ?: JSONArray().put(
                    JSONObject().put("role", "user").put("parts", partes)
                )
            )
        val config = JSONObject().put(
            "thinkingConfig",
            JSONObject().put("thinkingLevel", "LOW")
        )
        if (json) config.put("responseMimeType", "application/json")
        cuerpo.put("generationConfig", config)

        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$MODELO:generateContent?key=$clave"
        )
        val conexion = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connectTimeout = 60000
            readTimeout = 90000
        }
        conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
        val codigo = conexion.responseCode
        val respuesta = if (codigo in 200..299) {
            conexion.inputStream.bufferedReader().use { it.readText() }
        } else {
            conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        if (codigo !in 200..299) {
            throw IllegalStateException(mensajeError(codigo, respuesta))
        }
        return textoDeRespuesta(JSONObject(respuesta))
    }

    private fun textoDeRespuesta(jsonRespuesta: JSONObject): String {
        val candidatos = jsonRespuesta.optJSONArray("candidates")
            ?: throw IllegalStateException("Gemini no devolvió una respuesta.")
        val partes = candidatos.getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
        val textos = buildList {
            for (i in 0 until partes.length()) {
                val parte = partes.getJSONObject(i)
                if (parte.optBoolean("thought")) continue
                val texto = parte.optString("text")
                if (texto.isNotBlank()) add(texto)
            }
        }
        return textos.lastOrNull()?.trim()
            ?: throw IllegalStateException("Gemini no devolvió un texto.")
    }

    private fun sugerenciaDesde(texto: String): SugerenciaGasto {
        val limpio = texto
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = JSONObject(limpio)
        val nombre = json.optString("category")
        val categoria = CategoriaGasto.entries.find {
            it.name.equals(nombre, ignoreCase = true) ||
                    it.descripcion.equals(nombre, ignoreCase = true)
        }
        val amount = when {
            !json.has("amount") || json.isNull("amount") -> null
            else -> json.optDouble("amount").takeIf { !it.isNaN() && it > 0.0 }
        }
        val lugar = json.optString("place").takeIf { it.isNotBlank() && it != "null" }
        return SugerenciaGasto(
            amount = amount,
            category = categoria,
            description = json.optString("description"),
            placeLabel = lugar
        )
    }

    private fun mensajeError(codigo: Int, cuerpo: String): String {
        val detalle = runCatching {
            JSONObject(cuerpo).getJSONObject("error").optString("message")
        }.getOrNull().orEmpty()

        return when (codigo) {
            400, 403 -> "La clave de acceso no es válida. Revisá la configuración."
            429 -> "El asistente está recibiendo muchas consultas. Probá de nuevo en un rato."
            503 -> "El servidor tiene alta demanda en este momento. Por favor, intentá de nuevo en unos minutos." // Captura explícita del error en inglés
            500 -> "Ocurrió un error en el servidor. Intentá más tarde."
            else -> "Ocurrió un error inesperado. Por favor, intentá de nuevo."
        }
    }
}