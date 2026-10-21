package com.example.tickettrack.model

data class SugerenciaGasto(
    val amount: Double?,
    val category: CategoriaGasto?,
    val description: String,
    val placeLabel: String?
)
