package com.example.tickettrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tickettrack.ui.theme.TicketTrackTheme
import com.example.tickettrack.view.AddGastoScreen
import com.example.tickettrack.view.HomeScreen
import com.example.tickettrack.view.LoginScreen
import com.example.tickettrack.viewmodel.TicketTrackViewModel

class MainActivity : ComponentActivity() {

    // 1. Pedimos el ViewModel al sistema
    private val viewModel: TicketTrackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    var pantallaActual by rememberSaveable { mutableStateOf("home") }

    if (usuario == null) {
        LoginScreen(viewModel)
    } else {
        // Si hay sesión iniciada, decidimos qué pantalla mostrar según el estado
        when (pantallaActual) {
            "home" -> HomeScreen(
                viewModel = viewModel,
                onNavegarAddGasto = { pantallaActual = "add" }
            )
            "add" -> AddGastoScreen(
                viewModel = viewModel,
                onGastoAgregado = { pantallaActual = "home" },
                onVolver = { pantallaActual = "home" }
            )
        }
    }
}

