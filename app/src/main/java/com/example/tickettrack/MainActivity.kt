package com.example.tickettrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudinary.android.MediaManager
import com.example.tickettrack.ui.theme.TicketTrackTheme
import com.example.tickettrack.view.AddGastoScreen
import com.example.tickettrack.view.DetailGastoScreen
import com.example.tickettrack.view.HomeScreen
import com.example.tickettrack.view.LoginScreen
import com.example.tickettrack.view.ResumenScreen
import com.example.tickettrack.viewmodel.TicketTrackViewModel

class MainActivity : ComponentActivity() {

    // 1. Pedimos el ViewModel al sistema
    private val viewModel: TicketTrackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // INICIALIZACIÓN DE CLOUDINARY
        try {
            val config = mapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
            )
            MediaManager.init(this, config)
        } catch (e: Exception) {
            // Si ya se inicializó (ej: al girar la pantalla), lo ignoramos silenciosamente
        }

        setContent {
            TicketTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EnrutadorDeSesion(viewModel)
                }
            }
        }
    }
}

@Composable
fun EnrutadorDeSesion(viewModel: TicketTrackViewModel) {
    val usuario by viewModel.usuario.collectAsStateWithLifecycle()

    // Qué pantalla está abierta, y cuál gasto se eligió en el listado.
    var pantallaActual by rememberSaveable { mutableStateOf("home") }
    var gastoIdSeleccionado by rememberSaveable { mutableStateOf<String?>(null) }

    // Al cerrar sesión volvemos al inicio, para no reabrir el detalle del usuario anterior.
    LaunchedEffect(usuario) {
        if (usuario == null) {
            pantallaActual = "home"
            gastoIdSeleccionado = null
        }
    }

    if (usuario == null) {
        LoginScreen(viewModel)
    } else {
        when (pantallaActual) {
            "home" -> HomeScreen(
                viewModel = viewModel,
                onNavegarAddGasto = { pantallaActual = "add" },
                onAbrirGasto = { id ->
                    gastoIdSeleccionado = id
                    pantallaActual = "detail"
                },
                onAbrirResumen = { pantallaActual = "resumen" }
            )
            "resumen" -> ResumenScreen(
                viewModel = viewModel,
                onVolver = { pantallaActual = "home" }
            )
            "add" -> AddGastoScreen(
                viewModel = viewModel,
                gastoId = null,
                onGuardado = { pantallaActual = "home" },
                onVolver = { pantallaActual = "home" }
            )
            "detail" -> DetailGastoScreen(
                viewModel = viewModel,
                gastoId = gastoIdSeleccionado.orEmpty(),
                onVolver = {
                    pantallaActual = "home"
                    gastoIdSeleccionado = null
                },
                onEditar = { pantallaActual = "edit" },
                onEliminado = {
                    pantallaActual = "home"
                    gastoIdSeleccionado = null
                }
            )
            "edit" -> AddGastoScreen(
                viewModel = viewModel,
                gastoId = gastoIdSeleccionado,
                onGuardado = { pantallaActual = "detail" },
                onVolver = { pantallaActual = "detail" }
            )
        }
    }
}

