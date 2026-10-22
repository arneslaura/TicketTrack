package com.example.tickettrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = VerdeBrillo,
    onPrimary = Color(0xFF04281C),
    primaryContainer = Color(0xFF0F3D30),
    onPrimaryContainer = Color(0xFFB6F5DC),
    secondary = VerdeClaro,
    tertiary = VerdeClaro,
    background = FondoNoche,
    surface = SuperficieNoche,
    surfaceVariant = Color(0xFF24312C),
    surfaceContainerLowest = Color(0xFF090E0C),
    surfaceContainerLow = Color(0xFF121916),
    surfaceContainer = Color(0xFF18211D),
    surfaceContainerHigh = Color(0xFF1F2A25),
    surfaceContainerHighest = Color(0xFF26332D),
    onBackground = TextoClaro,
    onSurface = TextoClaro,
    onSurfaceVariant = TextoSuaveNoche,
    outline = Color(0xFF3A4A44),
    outlineVariant = Color(0xFF2A3833),
    error = ErrorRojo
)

private val LightColorScheme = lightColorScheme(
    primary = VerdePrincipal,
    onPrimary = Color.White,
    primaryContainer = VerdeClaro,
    onPrimaryContainer = VerdeOscuro,
    secondary = VerdeOscuro,
    tertiary = VerdeClaro,
    background = FondoClaro,
    surface = Color.White,
    surfaceVariant = Color(0xFFE4EEE9),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7FAF8),
    surfaceContainer = Color(0xFFEEF3F0),
    surfaceContainerHigh = Color(0xFFE6EEEA),
    surfaceContainerHighest = Color(0xFFDEE8E3),
    onBackground = TextoOscuro,
    onSurface = TextoOscuro,
    onSurfaceVariant = TextoSuave,
    outline = Color(0xFFD3DED8),
    outlineVariant = Color(0xFFE3EBE7),
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
        shapes = Formas,
        content = content
    )
}