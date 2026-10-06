package com.example.tickettrack.model

import java.util.Date

enum class CategoriaGasto(val descripcion: String) {
    SUPERMERCADO("Supermercado"),
    TRANSPORTE("Transporte"),
    SERVICIOS("Servicios"),
    OCIO("Ocio"),
    SALUD("Salud"),
    OTROS("Otros")
}

data class Gasto(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val category: CategoriaGasto = CategoriaGasto.OTROS,
    val description: String = "",
    val photoUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeLabel: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)