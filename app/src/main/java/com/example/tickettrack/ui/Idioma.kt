package com.example.tickettrack.ui

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.tickettrack.model.CategoriaGasto
import com.example.tickettrack.model.PeriodoFiltro
import java.util.Locale

enum class Idioma(val etiqueta: String, val locale: Locale) {
    ESPANOL("Español", Locale("es", "AR")),
    INGLES("English", Locale.ENGLISH),
    PORTUGUES("Português", Locale("pt", "BR"))
}

val LocalIdioma = staticCompositionLocalOf { Idioma.ESPANOL }

object Idiomas {
    private const val PREFS = "tickettrack_idioma"
    private const val CLAVE = "idioma"

    fun leer(context: Context): Idioma {
        val nombre = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CLAVE, Idioma.ESPANOL.name)
        return Idioma.entries.find { it.name == nombre } ?: Idioma.ESPANOL
    }

    fun guardar(context: Context, idioma: Idioma): Idioma {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CLAVE, idioma.name)
            .apply()
        return idioma
    }
}

// ESTO SE SIGUE USANDO: Traduce las categorías del modelo fuera de los Composables
fun CategoriaGasto.en(idioma: Idioma): String = when (idioma) {
    Idioma.ESPANOL -> descripcion
    Idioma.INGLES -> when (this) {
        CategoriaGasto.SUPERMERCADO -> "Groceries"
        CategoriaGasto.TRANSPORTE -> "Transport"
        CategoriaGasto.SERVICIOS -> "Bills"
        CategoriaGasto.OCIO -> "Leisure"
        CategoriaGasto.SALUD -> "Health"
        CategoriaGasto.OTROS -> "Other"
    }
    Idioma.PORTUGUES -> when (this) {
        CategoriaGasto.SUPERMERCADO -> "Supermercado"
        CategoriaGasto.TRANSPORTE -> "Transporte"
        CategoriaGasto.SERVICIOS -> "Serviços"
        CategoriaGasto.OCIO -> "Lazer"
        CategoriaGasto.SALUD -> "Saúde"
        CategoriaGasto.OTROS -> "Outros"
    }
}

fun PeriodoFiltro.en(idioma: Idioma): String = when (idioma) {
    Idioma.ESPANOL -> etiqueta
    Idioma.INGLES -> when (this) {
        PeriodoFiltro.TODOS -> "All"
        PeriodoFiltro.SEMANA -> "This week"
        PeriodoFiltro.MES -> "This month"
    }
    Idioma.PORTUGUES -> when (this) {
        PeriodoFiltro.TODOS -> "Todos"
        PeriodoFiltro.SEMANA -> "Esta semana"
        PeriodoFiltro.MES -> "Este mês"
    }
}
