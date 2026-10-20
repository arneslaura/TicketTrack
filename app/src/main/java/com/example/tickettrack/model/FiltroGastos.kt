package com.example.tickettrack.model

import java.util.Calendar

enum class PeriodoFiltro(val etiqueta: String) {
    TODOS("Todos"),
    SEMANA("Esta semana"),
    MES("Este mes")
}

fun filtrarGastos(
    gastos: List<Gasto>,
    categoria: CategoriaGasto?,
    periodo: PeriodoFiltro,
    ahora: Calendar = Calendar.getInstance()
): List<Gasto> {
    val desde = when (periodo) {
        PeriodoFiltro.TODOS -> null
        PeriodoFiltro.SEMANA -> inicioDeSemana(ahora).timeInMillis
        PeriodoFiltro.MES -> inicioDeMes(ahora).timeInMillis
    }
    return gastos.filter { gasto ->
        val categoriaOk = categoria == null || gasto.category == categoria
        val periodoOk = desde == null || gasto.createdAt.time >= desde
        categoriaOk && periodoOk
    }
}

private fun inicioDeSemana(ahora: Calendar): Calendar {
    val inicio = ahora.clone() as Calendar
    val diff = (inicio.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    inicio.add(Calendar.DAY_OF_YEAR, -diff)
    inicio.set(Calendar.HOUR_OF_DAY, 0)
    inicio.set(Calendar.MINUTE, 0)
    inicio.set(Calendar.SECOND, 0)
    inicio.set(Calendar.MILLISECOND, 0)
    return inicio
}

private fun inicioDeMes(ahora: Calendar): Calendar {
    val inicio = ahora.clone() as Calendar
    inicio.set(Calendar.DAY_OF_MONTH, 1)
    inicio.set(Calendar.HOUR_OF_DAY, 0)
    inicio.set(Calendar.MINUTE, 0)
    inicio.set(Calendar.SECOND, 0)
    inicio.set(Calendar.MILLISECOND, 0)
    return inicio
}
