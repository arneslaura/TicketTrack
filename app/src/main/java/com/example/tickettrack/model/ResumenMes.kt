package com.example.tickettrack.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class LineaResumen(
    val categoria: CategoriaGasto,
    val total: Double,
    val proporcion: Float
)

data class ResumenMes(
    val titulo: String,
    val total: Double,
    val cantidad: Int,
    val porCategoria: List<LineaResumen>
)

fun resumenDelMes(gastos: List<Gasto>, ahora: Calendar = Calendar.getInstance()): ResumenMes {
    val delMes = gastos.filter { mismoMes(it.createdAt, ahora) }
    val total = delMes.sumOf { it.amount }
    val lineas = delMes
        .groupBy { it.category }
        .map { (categoria, items) ->
            val suma = items.sumOf { it.amount }
            LineaResumen(
                categoria = categoria,
                total = suma,
                proporcion = if (total > 0.0) (suma / total).toFloat() else 0f
            )
        }
        .sortedByDescending { it.total }

    val formato = SimpleDateFormat("MMMM yyyy", Locale("es", "AR"))
    val titulo = formato.format(ahora.time).replaceFirstChar { it.titlecase(Locale("es", "AR")) }

    return ResumenMes(
        titulo = titulo,
        total = total,
        cantidad = delMes.size,
        porCategoria = lineas
    )
}

fun formatoPesos(monto: Double): String {
    var entero = monto.toLong()
    var centavos = ((monto - entero) * 100).roundToInt()
    if (centavos == 100) {
        entero += 1
        centavos = 0
    }
    val miles = "%,d".format(Locale("es", "AR"), entero)
    return if (centavos == 0) "$$miles" else "$$miles,%02d".format(centavos)
}

private fun mismoMes(fecha: Date, referencia: Calendar): Boolean {
    val calendario = Calendar.getInstance().apply { time = fecha }
    return calendario.get(Calendar.YEAR) == referencia.get(Calendar.YEAR) &&
        calendario.get(Calendar.MONTH) == referencia.get(Calendar.MONTH)
}
