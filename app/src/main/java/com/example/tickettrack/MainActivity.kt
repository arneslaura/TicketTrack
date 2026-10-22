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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudinary.android.MediaManager
import com.example.tickettrack.ui.LocalIdioma
import com.example.tickettrack.ui.texto
import com.example.tickettrack.ui.theme.ModoTema
import com.example.tickettrack.ui.theme.TicketTrackTheme
import com.example.tickettrack.view.AddGastoScreen
import com.example.tickettrack.view.AsistenteScreen
import com.example.tickettrack.view.DetailGastoScreen
import com.example.tickettrack.view.HomeScreen
import com.example.tickettrack.view.LoginScreen
import com.example.tickettrack.view.MapaGastosScreen
import com.example.tickettrack.view.PerfilScreen
import com.example.tickettrack.view.ResumenScreen
import com.example.tickettrack.viewmodel.TicketTrackViewModel

// IMPORTANTE: Este import es nuevo para configurar el mapa
import org.osmdroid.config.Configuration

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

        // --- INICIALIZACIÓN DE OPENSTREETMAP ---
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        org.osmdroid.config.Configuration.getInstance().load(applicationContext, prefs)
        org.osmdroid.config.Configuration.getInstance().userAgentValue =
            "TicketTrack/1.0 (com.example.tickettrack; Android)"
        // ---------------------------------------

        setContent {
            val modoTema by viewModel.modoTema.collectAsStateWithLifecycle()
            val idioma by viewModel.idioma.collectAsStateWithLifecycle()
            val oscuro = when (modoTema) {
                ModoTema.SISTEMA -> isSystemInDarkTheme()
                ModoTema.CLARO -> false
                ModoTema.OSCURO -> true
            }
            CompositionLocalProvider(LocalIdioma provides idioma) {
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

    val esPestana = pantallaActual in setOf("home", "mapa", "resumen", "asistente", "perfil")

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

    val idioma = LocalIdioma.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (pantallaActual == "home") {
                FloatingActionButton(
                    onClick = { pantallaActual = "add" },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo gasto")
                }
            }
        },
        bottomBar = {
            val coloresNav = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp
            ) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0.dp)
            ) {
                NavigationBarItem(
                    selected = pantallaActual == "home",
                    onClick = { pantallaActual = "home" },
                    colors = coloresNav,
                    icon = { Icon(Icons.Filled.Receipt, contentDescription = null) },
                    label = { Text(texto(idioma, "Gastos", "Expenses", "Gastos")) }
                )
                NavigationBarItem(
                    selected = pantallaActual == "mapa",
                    onClick = { pantallaActual = "mapa" },
                    colors = coloresNav,
                    icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                    label = { Text(texto(idioma, "Mapa", "Map", "Mapa")) }
                )
                NavigationBarItem(
                    selected = pantallaActual == "resumen",
                    onClick = { pantallaActual = "resumen" },
                    colors = coloresNav,
                    icon = { Icon(Icons.Filled.PieChart, contentDescription = null) },
                    label = { Text(texto(idioma, "Resumen", "Summary", "Resumo")) }
                )
                NavigationBarItem(
                    selected = pantallaActual == "asistente",
                    onClick = { pantallaActual = "asistente" },
                    colors = coloresNav,
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                    label = { Text("IA") }
                )
                NavigationBarItem(
                    selected = pantallaActual == "perfil",
                    onClick = { pantallaActual = "perfil" },
                    colors = coloresNav,
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text(texto(idioma, "Perfil", "Profile", "Perfil")) }
                )
            }
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
                "asistente" -> AsistenteScreen(viewModel)
                "resumen" -> ResumenScreen(
                    viewModel = viewModel,
                    onAbrirGasto = { id -> abrirGasto(id, "resumen") }
                )
                "perfil" -> PerfilScreen(viewModel)
            }
        }
    }
}

