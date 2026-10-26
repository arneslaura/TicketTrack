package com.example.tickettrack.model

import com.example.tickettrack.data.textoUbicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class LogicaGastoTest {

    @Test
    fun formatoPesos_agrupaMilesYCentavos() {
        assertEquals("$0", formatoPesos(0.0))
        assertEquals("$1.500", formatoPesos(1500.0))
        assertEquals("$1.500,50", formatoPesos(1500.5))
        assertEquals("$11", formatoPesos(10.996))
    }

    @Test
    fun filtrarGastos_porPeriodoSemana() {
        val ahora = fecha(2026, Calendar.OCTOBER, 21, 15) // Un miércoles
        val gastoEnSemana = gasto(150.0, CategoriaGasto.TRANSPORTE, fecha(2026, Calendar.OCTOBER, 19, 10)) // Lunes de esa semana
        val gastoFueraSemana = gasto(40.0, CategoriaGasto.SUPERMERCADO, fecha(2026, Calendar.OCTOBER, 10, 23)) // Semana anterior
        val lista = listOf(gastoEnSemana, gastoFueraSemana)

        val resultado = filtrarGastos(lista, null, PeriodoFiltro.SEMANA, ahora)

        assertEquals(1, resultado.size)
        assertEquals(gastoEnSemana, resultado.first())
    }

    @Test
    fun filtrarGastos_porCategoriaYPeriodo() {
        val ahora = fecha(2026, Calendar.OCTOBER, 21, 15)
        val superLunes = gasto(100.0, CategoriaGasto.SUPERMERCADO, fecha(2026, Calendar.OCTOBER, 19, 10))
        val transporteHoy = gasto(300.0, CategoriaGasto.TRANSPORTE, fecha(2026, Calendar.OCTOBER, 21, 9))
        val domingo = gasto(40.0, CategoriaGasto.SUPERMERCADO, fecha(2026, Calendar.OCTOBER, 18, 23))
        val mesPasado = gasto(50.0, CategoriaGasto.SUPERMERCADO, fecha(2026, Calendar.SEPTEMBER, 30, 12))
        val lista = listOf(superLunes, transporteHoy, domingo, mesPasado)

        val soloSuper = filtrarGastos(lista, CategoriaGasto.SUPERMERCADO, PeriodoFiltro.TODOS, ahora)
        assertEquals(listOf(superLunes, domingo, mesPasado), soloSuper)

        val estaSemana = filtrarGastos(lista, null, PeriodoFiltro.SEMANA, ahora)
        assertEquals(listOf(superLunes, transporteHoy), estaSemana)

        val superDelMes = filtrarGastos(lista, CategoriaGasto.SUPERMERCADO, PeriodoFiltro.MES, ahora)
        assertEquals(listOf(superLunes, domingo), superDelMes)
    }

    @Test
    fun resumenDelMes_sumaPromediaYComparaConElAnterior() {
        val ahora = fecha(2026, Calendar.OCTOBER, 15, 12)
        val chico = gasto(100.0, CategoriaGasto.SUPERMERCADO, fecha(2026, Calendar.OCTOBER, 2, 11))
        val grande = gasto(300.0, CategoriaGasto.TRANSPORTE, fecha(2026, Calendar.OCTOBER, 10, 11))
        val anterior = gasto(50.0, CategoriaGasto.OCIO, fecha(2026, Calendar.SEPTEMBER, 20, 11))

        val resumen = resumenDelMes(listOf(chico, grande, anterior), ahora)

        assertEquals("Octubre 2026", resumen.titulo)
        assertEquals(400.0, resumen.total, 0.001)

        // CORREGIDO: Validamos que existan 2 categorías distintas en el resumen de este mes (Supermercado y Transporte)
        assertEquals(2, resumen.porCategoria.size)

        val diasEnOctubre = ahora.getActualMaximum(Calendar.DAY_OF_MONTH) // 31 días
        val promedioEsperado = 400.0 / diasEnOctubre
        assertEquals(promedioEsperado, resumen.promedioDiario, 0.001)

        assertEquals(grande, resumen.mayor)
        assertEquals(50.0, resumen.totalMesAnterior, 0.001)
        assertEquals(350.0, resumen.diferenciaMesAnterior, 0.001)
        assertEquals(CategoriaGasto.TRANSPORTE, resumen.porCategoria.first().categoria)
        assertEquals(0.75f, resumen.porCategoria.first().proporcion, 0.001f)
    }

    @Test
    fun resumenDelMes_vacioNoFalla() {
        val ahora = fecha(2026, Calendar.OCTOBER, 1, 8)
        val resumen = resumenDelMes(emptyList(), ahora)

        assertEquals(0.0, resumen.total, 0.001)
        assertEquals(0.0, resumen.promedioDiario, 0.001)
        assertEquals(0.0, resumen.totalMesAnterior, 0.001)
        assertEquals(0.0, resumen.diferenciaMesAnterior, 0.001)
        assertNull(resumen.mayor)
        assertTrue(resumen.porCategoria.isEmpty())
    }

    @Test
    fun textoUbicacion_prefiereElNombreYSiNoLasCoordenadas() {
        assertEquals("Corrientes, Buenos Aires", textoUbicacion("Corrientes, Buenos Aires", 1.0, 2.0))
        assertEquals("-34.60370, -58.38160", textoUbicacion("  ", -34.6037, -58.3816))
        assertNull(textoUbicacion(null, null, null))
    }

    private fun gasto(amount: Double, categoria: CategoriaGasto, cuando: Calendar): Gasto {
        return Gasto(
            id = "$amount-${categoria.name}-${cuando.timeInMillis}",
            amount = amount,
            category = categoria,
            createdAt = Date(cuando.timeInMillis)
        )
    }

    private fun fecha(anio: Int, mes: Int, dia: Int, hora: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, anio)
            set(Calendar.MONTH, mes)
            set(Calendar.DAY_OF_MONTH, dia)
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}