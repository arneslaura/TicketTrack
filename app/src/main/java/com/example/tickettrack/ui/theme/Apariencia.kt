package com.example.tickettrack.ui.theme

import android.content.Context

enum class ModoTema(val etiqueta: String) {
    SISTEMA("Como el celular"),
    CLARO("Día"),
    OSCURO("Noche")
}

object Apariencia {
    private const val PREFS = "tickettrack_apariencia"
    private const val CLAVE = "modo"

    fun leer(context: Context): ModoTema {
        val nombre = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CLAVE, ModoTema.SISTEMA.name)
        return ModoTema.entries.find { it.name == nombre } ?: ModoTema.SISTEMA
    }

    fun guardar(context: Context, modo: ModoTema): ModoTema {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CLAVE, modo.name)
            .apply()
        return modo
    }
}
