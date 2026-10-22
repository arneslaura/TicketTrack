package com.example.tickettrack.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.tickettrack.ui.Idioma
import com.example.tickettrack.ui.texto
import com.example.tickettrack.ui.theme.ModoTema
import com.example.tickettrack.viewmodel.TicketTrackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(viewModel: TicketTrackViewModel) {
    val context = LocalContext.current
    val usuario by viewModel.usuario.collectAsStateWithLifecycle()
    val horaRecordatorio by viewModel.horaRecordatorio.collectAsStateWithLifecycle()
    val modoTema by viewModel.modoTema.collectAsStateWithLifecycle()
    val idioma by viewModel.idioma.collectAsStateWithLifecycle()

    var dialogoRecordatorio by rememberSaveable { mutableStateOf(false) }
    var avisoNotificacion by rememberSaveable { mutableStateOf<String?>(null) }
    var probarAlPermitir by remember { mutableStateOf(false) }

    val permisoNotificacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            avisoNotificacion = null
            if (probarAlPermitir) viewModel.probarRecordatorio()
        } else {
            avisoNotificacion = "Activá las notificaciones para usar el recordatorio."
            viewModel.cambiarHoraRecordatorio(horaRecordatorio.hora, horaRecordatorio.minuto, false)
        }
        probarAlPermitir = false
    }

    fun notificacionesPermitidas(): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }

    // Dividimos la pantalla en una columna principal
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // PARTE SUPERIOR (Scrolleable)
        Column(
            modifier = Modifier
                .weight(1f) // Esto empuja al botón de abajo hacia el fondo
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. TARJETA DE USUARIO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (usuario?.photoUrl != null) {
                        AsyncImage(
                            model = usuario?.photoUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Perfil genérico",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val nombre = usuario?.displayName
                    if (!nombre.isNullOrBlank()) {
                        Text(
                            text = nombre,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = usuario?.email ?: "Sesión iniciada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 2. SECCIÓN CONFIGURACIÓN
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Configuración",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        // APARIENCIA
                        Text(text = texto(idioma, "Apariencia", "Appearance", "Aparência"), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BotonApariencia(
                                texto = texto(idioma, "Sistema", "System", "Sistema"),
                                elegido = modoTema == ModoTema.SISTEMA,
                                icono = Icons.Filled.PhoneAndroid,
                                onClick = { viewModel.cambiarModoTema(ModoTema.SISTEMA) },
                                modifier = Modifier.weight(1f)
                            )
                            BotonApariencia(
                                texto = texto(idioma, "Día", "Day", "Dia"),
                                elegido = modoTema == ModoTema.CLARO,
                                icono = Icons.Filled.WbSunny,
                                onClick = { viewModel.cambiarModoTema(ModoTema.CLARO) },
                                modifier = Modifier.weight(1f)
                            )
                            BotonApariencia(
                                texto = texto(idioma, "Noche", "Night", "Noite"),
                                elegido = modoTema == ModoTema.OSCURO,
                                icono = Icons.Filled.DarkMode,
                                onClick = { viewModel.cambiarModoTema(ModoTema.OSCURO) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = texto(idioma, "Idioma", "Language", "Idioma"), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Idioma.entries.forEach { opcion ->
                                OutlinedButton(
                                    onClick = { viewModel.cambiarIdioma(opcion) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (idioma == opcion) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(opcion.etiqueta, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // RECORDATORIO
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(texto(idioma, "Recordatorio diario", "Daily reminder", "Lembrete diário"), style = MaterialTheme.typography.titleMedium)
                                Text(texto(idioma, "Avisar para cargar gastos", "Remind me to add expenses", "Avisar para registrar gastos"), style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = horaRecordatorio.activo,
                                onCheckedChange = { encender ->
                                    if (encender && !notificacionesPermitidas()) {
                                        permisoNotificacion.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.cambiarHoraRecordatorio(horaRecordatorio.hora, horaRecordatorio.minuto, encender)
                                    }
                                }
                            )
                        }

                        // Si está encendido, mostramos a qué hora
                        if (horaRecordatorio.activo) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { dialogoRecordatorio = true }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(texto(idioma, "Hora del aviso", "Reminder time", "Hora do aviso"), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = horaRecordatorio.texto(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (avisoNotificacion != null) {
                                Text(text = avisoNotificacion!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // PARTE INFERIOR (Fija al fondo)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.salir() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(texto(idioma, "Cerrar sesión", "Sign out", "Sair"))
            }
        }
    }

    if (dialogoRecordatorio) {
        DialogoRecordatorio(
            hora = horaRecordatorio.hora,
            minuto = horaRecordatorio.minuto,
            onGuardar = { hora, minuto ->
                viewModel.cambiarHoraRecordatorio(hora, minuto, true)
                dialogoRecordatorio = false
            },
            onProbar = {
                if (notificacionesPermitidas()) {
                    avisoNotificacion = null
                    viewModel.probarRecordatorio()
                } else if (Build.VERSION.SDK_INT >= 33) {
                    probarAlPermitir = true
                    permisoNotificacion.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onCerrar = { dialogoRecordatorio = false }
        )
    }
}

@Composable
private fun BotonApariencia(
    texto: String,
    elegido: Boolean,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (elegido) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(texto, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoRecordatorio(
    hora: Int,
    minuto: Int,
    onGuardar: (Int, Int) -> Unit,
    onProbar: () -> Unit,
    onCerrar: () -> Unit
) {
    val estado = rememberTimePickerState(
        initialHour = hora,
        initialMinute = minuto,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Hora del aviso") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimeInput(state = estado)
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onProbar) { Text("Hacer prueba de sonido") }
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(estado.hour, estado.minute) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}
