package com.example.tickettrack.data

import com.example.tickettrack.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

object FotosCloudinary {

    fun subir(bytes: ByteArray, uid: String, gastoId: String): String {
        val publicId = "tickettrack/$uid/$gastoId"
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val parametros = mapOf(
            "overwrite" to "true",
            "public_id" to publicId,
            "timestamp" to timestamp
        )
        val cuerpo = subirArchivo(
            ruta = "image/upload",
            bytes = bytes,
            nombreArchivo = "$gastoId.jpg",
            campos = parametros + mapOf(
                "api_key" to apiKey(),
                "signature" to firmar(parametros)
            )
        )
        return leerTexto(cuerpo, "secure_url")
            ?.replace("\\/", "/")
            ?: throw IllegalStateException("Cloudinary no devolvió la dirección de la foto.")
    }

    fun borrar(uid: String, gastoId: String) {
        val publicId = "tickettrack/$uid/$gastoId"
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val parametros = mapOf(
            "public_id" to publicId,
            "timestamp" to timestamp
        )
        val campos = parametros + mapOf(
            "api_key" to apiKey(),
            "signature" to firmar(parametros)
        )
        val conexion = abrir("image/destroy")
        conexion.outputStream.use { salida ->
            val cuerpo = campos.entries.joinToString("&") { (clave, valor) ->
                "$clave=${java.net.URLEncoder.encode(valor, "UTF-8")}"
            }
            salida.write(cuerpo.toByteArray())
        }
        val codigo = conexion.responseCode
        if (codigo !in 200..299) {
            val error = conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IllegalStateException(mensajeDeError(error))
        }
    }

    private fun subirArchivo(
        ruta: String,
        bytes: ByteArray,
        nombreArchivo: String,
        campos: Map<String, String>
    ): String {
        val limite = "----TicketTrack${UUID.randomUUID()}"
        val conexion = abrir(ruta)
        conexion.setRequestProperty("Content-Type", "multipart/form-data; boundary=$limite")
        conexion.outputStream.use { salida ->
            campos.forEach { (nombre, valor) ->
                salida.write("--$limite\r\n".toByteArray())
                salida.write("Content-Disposition: form-data; name=\"$nombre\"\r\n\r\n".toByteArray())
                salida.write(valor.toByteArray())
                salida.write("\r\n".toByteArray())
            }
            salida.write("--$limite\r\n".toByteArray())
            salida.write(
                "Content-Disposition: form-data; name=\"file\"; filename=\"$nombreArchivo\"\r\n"
                    .toByteArray()
            )
            salida.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
            salida.write(bytes)
            salida.write("\r\n--$limite--\r\n".toByteArray())
        }
        val codigo = conexion.responseCode
        val cuerpo = if (codigo in 200..299) {
            conexion.inputStream.bufferedReader().use { it.readText() }
        } else {
            conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        if (codigo !in 200..299) {
            throw IllegalStateException(mensajeDeError(cuerpo))
        }
        return cuerpo
    }

    private fun abrir(ruta: String): HttpURLConnection {
        val cloud = cloudName()
        val url = URL("https://api.cloudinary.com/v1_1/$cloud/$ruta")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20000
            readTimeout = 30000
        }
    }

    private fun firmar(parametros: Map<String, String>): String {
        val base = parametros.toSortedMap().entries.joinToString("&") { "${it.key}=${it.value}" }
        val texto = base + apiSecret()
        val digest = MessageDigest.getInstance("SHA-1").digest(texto.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun cloudName(): String = requerido(BuildConfig.CLOUDINARY_CLOUD_NAME, "CLOUDINARY_CLOUD_NAME")

    private fun apiKey(): String = requerido(BuildConfig.CLOUDINARY_API_KEY, "CLOUDINARY_API_KEY")

    private fun apiSecret(): String = requerido(BuildConfig.CLOUDINARY_API_SECRET, "CLOUDINARY_API_SECRET")

    private fun requerido(valor: String, nombre: String): String {
        if (valor.isBlank()) {
            throw IllegalStateException("Falta $nombre en local.properties.")
        }
        return valor
    }

    private fun leerTexto(json: String, campo: String): String? {
        return Regex("\"$campo\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
    }

    private fun mensajeDeError(cuerpo: String): String {
        val detalle = leerTexto(cuerpo, "message")?.replace("\\/", "/")
        return if (detalle.isNullOrBlank()) {
            "No se pudo subir la foto a Cloudinary."
        } else {
            "No se pudo subir la foto. $detalle"
        }
    }
}
