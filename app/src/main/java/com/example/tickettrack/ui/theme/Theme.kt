package com.example.tickettrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VerdePrincipal,
    secondary = VerdeOscuro,
    tertiary = VerdeClaro,
    background = FondoNoche,
    surface = SuperficieNoche,
    onPrimary = Color.White,
    onBackground = TextoClaro,
    onSurface = TextoClaro,
    error = ErrorRojo
)

private val LightColorScheme = lightColorScheme(
    primary = VerdePrincipal,          // Color de los botones y bordes activos
    secondary = VerdeOscuro,
    tertiary = VerdeClaro,
    background = FondoClaro,           // Color de fondo de las pantallas
    surface = Color.White,             // Color de las tarjetas o menús
    onPrimary = Color.White,           // Color del texto que va dentro de un botón primario
    onBackground = TextoOscuro,        // Color del texto normal sobre el fondo
    error = ErrorRojo
)

@Composable
fun TicketTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val vista = LocalView.current
    if (!vista.isInEditMode) {
        SideEffect {
            val window = (vista.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controlador = WindowCompat.getInsetsController(window, vista)
            controlador.isAppearanceLightStatusBars = !darkTheme
            controlador.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}