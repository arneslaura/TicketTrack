package com.example.tickettrack.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.tickettrack.model.CategoriaGasto
import androidx.compose.ui.res.stringResource
import com.example.tickettrack.R
import com.example.tickettrack.model.PeriodoFiltro

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

@Composable
fun CategoriaGasto.traducida(): String {
    return when (this) {
        CategoriaGasto.SUPERMERCADO -> stringResource(R.string.cat_supermarket)
        CategoriaGasto.TRANSPORTE -> stringResource(R.string.cat_transport)
        CategoriaGasto.SERVICIOS -> stringResource(R.string.cat_services)
        CategoriaGasto.OCIO -> stringResource(R.string.cat_leisure)
        CategoriaGasto.SALUD -> stringResource(R.string.cat_health)
        CategoriaGasto.OTROS -> stringResource(R.string.cat_others)
    }
}

@Composable
fun PeriodoFiltro.traducido(): String {
    return when (this) {
        PeriodoFiltro.TODOS -> stringResource(R.string.filter_all)
        PeriodoFiltro.SEMANA -> stringResource(R.string.filter_week)
        PeriodoFiltro.MES -> stringResource(R.string.filter_month)
    }
}
