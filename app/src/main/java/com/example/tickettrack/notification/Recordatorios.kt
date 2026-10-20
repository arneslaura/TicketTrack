package com.example.tickettrack.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tickettrack.MainActivity
import com.example.tickettrack.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

// Agregamos el "activo" a la clase
data class HoraRecordatorio(val hora: Int, val minuto: Int, val activo: Boolean = false) {
    fun texto(): String = "%02d:%02d".format(hora, minuto)
}

object Recordatorios {
    private const val PREFS = "tickettrack_recordatorio"
    private const val CLAVE_HORA = "hora"
    private const val CLAVE_MINUTO = "minuto"
    private const val CLAVE_ACTIVO = "activo"
    private const val CANAL = "recordatorios"
    private const val TRABAJO = "recordatorio_diario"
    private const val NOTIFICACION_ID = 21

    fun leer(context: Context): HoraRecordatorio {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return HoraRecordatorio(
            hora = prefs.getInt(CLAVE_HORA, 21),
            minuto = prefs.getInt(CLAVE_MINUTO, 0),
            activo = prefs.getBoolean(CLAVE_ACTIVO, false) // Por defecto apagado
        )
    }

    fun guardar(context: Context, hora: Int, minuto: Int, activo: Boolean): HoraRecordatorio {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(CLAVE_HORA, hora)
            .putInt(CLAVE_MINUTO, minuto)
            .putBoolean(CLAVE_ACTIVO, activo)
            .apply()
        return HoraRecordatorio(hora, minuto, activo)
    }

    fun programar(context: Context) {
        val config = leer(context)
        if (config.activo) {
            encolar(context, ExistingWorkPolicy.REPLACE)
        } else {
            cancelar(context)
        }
    }

    fun programarSiguiente(context: Context) {
        val config = leer(context)
        if (config.activo) {
            encolar(context, ExistingWorkPolicy.APPEND)
        }
    }

    fun cancelar(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TRABAJO)
    }

    fun mostrar(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = NotificationManagerCompat.from(context)
        val canal = NotificationChannelCompat.Builder(CANAL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName("Recordatorios")
            .setDescription("Aviso diario para cargar los gastos")
            .build()
        manager.createNotificationChannel(canal)

        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_recordatorio)
            .setContentTitle("TicketTrack")
            .setContentText("¿Cargaste tus gastos de hoy?")
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICACION_ID, notificacion)
    }

    private fun encolar(context: Context, policy: ExistingWorkPolicy) {
        val hora = leer(context)
        val espera = millisHasta(hora.hora, hora.minuto).coerceAtLeast(1_000L)
        val pedido = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(espera, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(TRABAJO, policy, pedido)
    }

    private fun millisHasta(hora: Int, minuto: Int): Long {
        val ahora = Calendar.getInstance()
        val objetivo = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(ahora)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return objetivo.timeInMillis - ahora.timeInMillis
    }
}
