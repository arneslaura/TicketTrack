package com.example.tickettrack.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

fun crearArchivoTicket(context: Context): Uri {
    val carpeta = File(context.cacheDir, "tickets").apply { mkdirs() }
    val archivo = File(carpeta, "ticket_${System.currentTimeMillis()}.jpg")
    archivo.createNewFile()
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        archivo
    )
}

fun comprimirTicket(context: Context, uri: Uri): ByteArray {
    val resolver = context.contentResolver
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
    if (medidas.outWidth <= 0 || medidas.outHeight <= 0) {
        throw IllegalArgumentException("No se pudo leer la foto")
    }

    var sample = 1
    while (medidas.outWidth / sample > 1280 || medidas.outHeight / sample > 1280) {
        sample *= 2
    }

    val opciones = BitmapFactory.Options().apply { inSampleSize = sample }
    val original = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opciones)
    } ?: throw IllegalArgumentException("No se pudo leer la foto")

    val orientada = rotarSegunExif(context, uri, original)
    val salida = ByteArrayOutputStream()
    orientada.compress(Bitmap.CompressFormat.JPEG, 80, salida)
    if (orientada != original) original.recycle()
    orientada.recycle()
    return salida.toByteArray()
}

private fun rotarSegunExif(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    val orientacion = context.contentResolver.openInputStream(uri)?.use { input ->
        ExifInterface(input).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL

    val grados = when (orientacion) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
    }

    val matriz = Matrix().apply { postRotate(grados) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
}
