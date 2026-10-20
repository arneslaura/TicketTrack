package com.example.tickettrack


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudinary.android.MediaManager
import com.example.tickettrack.ui.theme.ModoTema
import com.example.tickettrack.ui.theme.TicketTrackTheme
import com.example.tickettrack.view.AddGastoScreen
import com.example.tickettrack.view.DetailGastoScreen
import com.example.tickettrack.view.HomeScreen
import com.example.tickettrack.view.LoginScreen
import com.example.tickettrack.view.MapaGastosScreen
import com.example.tickettrack.view.PerfilScreen
import com.example.tickettrack.view.ResumenScreen
import com.example.tickettrack.viewmodel.TicketTrackViewModel


class MainActivity : ComponentActivity() {

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
            val modoTema by viewModel.modoTema.collectAsStateWithLifecycle()
            val oscuro = when (modoTema) {
                ModoTema.SISTEMA -> isSystemInDarkTheme()
                ModoTema.CLARO -> false
                ModoTema.OSCURO -> true
            }
            TicketTrackTheme(darkTheme = oscuro) {
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
    val context = LocalContext.current
    val usuario by viewModel.usuario.collectAsStateWithLifecycle()

    var pantallaActual by rememberSaveable { mutableStateOf("home") }
    var gastoIdSeleccionado by rememberSaveable { mutableStateOf<String?>(null) }
    var origenDetalle by rememberSaveable { mutableStateOf("home") }

    val permisoNotificacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(usuario?.uid) {
        if (usuario == null) {
            pantallaActual = "home"
            gastoIdSeleccionado = null
            origenDetalle = "home"
        } else {
            viewModel.obtenerGastos()
            val faltaPermiso = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            if (faltaPermiso) {
                permisoNotificacion.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (usuario == null) {
        LoginScreen(viewModel)
        return
    }

    val esPestana = pantallaActual in setOf("home", "mapa", "resumen", "perfil")

    fun abrirGasto(id: String, origen: String) {
        gastoIdSeleccionado = id
        origenDetalle = origen
        pantallaActual = "detail"
    }

    fun volverAlOrigen() {
        pantallaActual = origenDetalle
        gastoIdSeleccionado = null
    }

    if (!esPestana) {
        when (pantallaActual) {
            "add" -> AddGastoScreen(
                viewModel = viewModel,
                gastoId = null,
                onGuardado = { pantallaActual = "home" },
                onVolver = { pantallaActual = "home" }
            )
            "detail" -> DetailGastoScreen(
                viewModel = viewModel,
                gastoId = gastoIdSeleccionado.orEmpty(),
                onVolver = { volverAlOrigen() },
                onEditar = { pantallaActual = "edit" },
                onEliminado = { volverAlOrigen() }
            )
            "edit" -> AddGastoScreen(
                viewModel = viewModel,
                gastoId = gastoIdSeleccionado,
                onGuardado = { pantallaActual = "detail" },
                onVolver = { pantallaActual = "detail" }
            )
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (pantallaActual == "home") {
                FloatingActionButton(
                    onClick = { pantallaActual = "add" },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo gasto")
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = pantallaActual == "home",
                    onClick = { pantallaActual = "home" },
                    icon = { Icon(Icons.Filled.Receipt, contentDescription = null) },
                    label = { Text("Gastos") }
                )
                NavigationBarItem(
                    selected = pantallaActual == "mapa",
                    onClick = { pantallaActual = "mapa" },
                    icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                    label = { Text("Mapa") }
                )
                NavigationBarItem(
                    selected = pantallaActual == "resumen",
                    onClick = { pantallaActual = "resumen" },
                    icon = { Icon(Icons.Filled.PieChart, contentDescription = null) },
                    label = { Text("Resumen") }
                )
                NavigationBarItem(
                    selected = pantallaActual == "perfil",
                    onClick = { pantallaActual = "perfil" },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Perfil") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (pantallaActual) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    onAbrirGasto = { id -> abrirGasto(id, "home") }
                )
                "mapa" -> MapaGastosScreen(
                    viewModel = viewModel,
                    onAbrirGasto = { id -> abrirGasto(id, "mapa") }
                )
                "resumen" -> ResumenScreen(
                    viewModel = viewModel,
                    onAbrirGasto = { id -> abrirGasto(id, "resumen") }
                )
                "perfil" -> PerfilScreen(viewModel)
            }
        }
    }
}
