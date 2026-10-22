package com.example.tickettrack.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.tickettrack.model.CategoriaGasto

fun iconoCategoria(categoria: CategoriaGasto): ImageVector {
    return when (categoria) {
        CategoriaGasto.SUPERMERCADO -> Icons.Filled.ShoppingCart
        CategoriaGasto.TRANSPORTE -> Icons.Filled.DirectionsBus
        CategoriaGasto.SERVICIOS -> Icons.Filled.ReceiptLong
        CategoriaGasto.OCIO -> Icons.Filled.LocalActivity
        CategoriaGasto.SALUD -> Icons.Filled.Favorite
        CategoriaGasto.OTROS -> Icons.Filled.Category
    }
}

fun colorCategoria(categoria: CategoriaGasto): Color {
    return when (categoria) {
        CategoriaGasto.SUPERMERCADO -> Color(0xFF0C8A62)
        CategoriaGasto.TRANSPORTE -> Color(0xFF2563EB)
        CategoriaGasto.SERVICIOS -> Color(0xFF7C3AED)
        CategoriaGasto.OCIO -> Color(0xFFEA580C)
        CategoriaGasto.SALUD -> Color(0xFFE11D48)
        CategoriaGasto.OTROS -> Color(0xFF64748B)
    }
}
