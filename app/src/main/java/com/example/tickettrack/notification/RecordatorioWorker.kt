package com.example.tickettrack.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class RecordatorioWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        Recordatorios.mostrar(applicationContext)
        Recordatorios.programarSiguiente(applicationContext)
        return Result.success()
    }
}
