package com.example.tickettrack.view

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.tickettrack.R
import com.example.tickettrack.data.crearArchivoTicket
import com.example.tickettrack.data.etiquetaDeLugar
import com.example.tickettrack.data.textoUbicacion
import com.example.tickettrack.model.CategoriaGasto
import com.example.tickettrack.model.SugerenciaGasto
import com.example.tickettrack.viewmodel.TicketTrackViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGastoScreen(
    viewModel: TicketTrackViewModel,
    gastoId: String?,
    onGuardado: () -> Unit,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val editando = gastoId != null

    val gastos by viewModel.gastos.collectAsStateWithLifecycle()
    val guardando by viewModel.guardando.collectAsStateWithLifecycle()
    val errorGasto by viewModel.errorGasto.collectAsStateWithLifecycle()

    var amount by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var categoriaNombre by rememberSaveable { mutableStateOf(CategoriaGasto.OTROS.name) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var formularioListo by rememberSaveable { mutableStateOf(!editando) }

    var fechaMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var mostrarDatePicker by rememberSaveable { mutableStateOf(false) }

    var fotoActualUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var fotoNueva by rememberSaveable { mutableStateOf<String?>(null) }
    var fotoCamara by rememberSaveable { mutableStateOf<String?>(null) }
    var avisoFoto by rememberSaveable { mutableStateOf<String?>(null) }

    var latitud by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitud by rememberSaveable { mutableStateOf<Double?>(null) }
    var lugar by rememberSaveable { mutableStateOf<String?>(null) }
    var buscandoUbicacion by rememberSaveable { mutableStateOf(false) }
    var avisoUbicacion by rememberSaveable { mutableStateOf<String?>(null) }

    var frase by rememberSaveable { mutableStateOf("") }
    var consultandoIa by rememberSaveable { mutableStateOf(false) }
    var avisoIa by rememberSaveable { mutableStateOf<String?>(null) }

    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()

    fun tomarUbicacionActual() {
        scope.launch {
            buscandoUbicacion = true
            avisoUbicacion = null
            try {
                val actual = fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    ?: fused.lastLocation.await()

                if (actual == null) {
                    avisoUbicacion = "No se obtuvo una ubicación. Activá el GPS."
                } else {
                    latitud = actual.latitude
                    longitud = actual.longitude
                    lugar = etiquetaDeLugar(actual.latitude, actual.longitude)
                }
            } catch (_: SecurityException) {
                avisoUbicacion = "Falta el permiso de ubicación."
            } catch (e: Exception) {
                avisoUbicacion = e.localizedMessage ?: "No se pudo obtener la ubicación."
            } finally {
                buscandoUbicacion = false
            }
        }
    }

    val permisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) tomarUbicacionActual() else avisoUbicacion = "Aceptá el permiso de ubicación para guardar el lugar."
    }

    val categoriaSeleccionada = CategoriaGasto.entries.find { it.name == categoriaNombre } ?: CategoriaGasto.OTROS
    val vistaPrevia = fotoNueva ?: fotoActualUrl

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            fotoNueva = fotoCamara
            avisoFoto = null
        }
    }

    val permisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = crearArchivoTicket(context)
            fotoCamara = uri.toString()
            camara.launch(uri)
        } else {
            avisoFoto = "Para usar la cámara, aceptá el permiso."
        }
    }

    val voz = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        val textos = resultado.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!textos.isNullOrEmpty()) {
            frase = textos.first()
            avisoIa = null
        }
    }

    fun iniciarDictado() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Contá el gasto")
        }
        try {
            voz.launch(intent)
        } catch (_: ActivityNotFoundException) {
            avisoIa = "Este dispositivo no reconoce la voz. Escribí el gasto."
        }
    }

    val permisoMicrofono = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) iniciarDictado() else avisoIa = "Para dictar, aceptá el permiso del micrófono."
    }

    fun aplicarSugerencia(sugerencia: SugerenciaGasto) {
        sugerencia.amount?.let { amount = montoParaEditar(it) }
        sugerencia.category?.let { categoriaNombre = it.name }
        if (sugerencia.description.isNotBlank()) description = sugerencia.description
        if (!sugerencia.placeLabel.isNullOrBlank()) lugar = sugerencia.placeLabel
        avisoIa = "Revisá los datos antes de guardar."
    }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            fotoNueva = uri.toString()
            avisoFoto = null
        }
    }

    LaunchedEffect(Unit) { viewModel.limpiarErrorGasto() }

    LaunchedEffect(gastoId, gastos) {
        if (formularioListo || gastoId == null) return@LaunchedEffect
        val gasto = gastos.find { it.id == gastoId } ?: return@LaunchedEffect

        amount = montoParaEditar(gasto.amount)
        description = gasto.description
        categoriaNombre = gasto.category.name
        fotoActualUrl = gasto.photoUrl
        latitud = gasto.latitude
        longitud = gasto.longitude
        lugar = gasto.placeLabel
        fechaMillis = gasto.createdAt.time
        formularioListo = true
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            TextButton(onClick = onVolver, enabled = !guardando) {
                Text(
                    text = "← Volver",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (editando) stringResource(R.string.title_edit_expense) else stringResource(R.string.title_new_expense),
                style = MaterialTheme.typography.headlineMedium
            )

            if (editando && !formularioListo) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.loading_expense))
                return@Column
            }

            Spacer(modifier = Modifier.height(24.dp))

            val fechaFormateada = remember(fechaMillis) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(fechaMillis))
            }

            if (mostrarDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = fechaMillis)
                DatePickerDialog(
                    onDismissRequest = { mostrarDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { fechaMillis = it }
                            mostrarDatePicker = false
                        }) { Text("Aceptar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            SeccionFormulario(
                titulo = stringResource(R.string.section_ai_assistant),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ai_assistant_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = frase,
                    onValueChange = { frase = it },
                    placeholder = { Text(stringResource(R.string.ai_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !guardando && !consultandoIa,
                    colors = coloresCampo(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val permitido = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (permitido) iniciarDictado() else permisoMicrofono.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        enabled = !guardando && !consultandoIa,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_dictate))
                    }
                    Button(
                        onClick = {
                            val texto = frase.trim()
                            if (texto.isEmpty()) return@Button
                            consultandoIa = true
                            avisoIa = null
                            viewModel.interpretarFrase(
                                frase = texto,
                                onListo = { sugerencia -> aplicarSugerencia(sugerencia); consultandoIa = false },
                                onError = { mensaje -> avisoIa = mensaje; consultandoIa = false }
                            )
                        },
                        enabled = frase.isNotBlank() && !guardando && !consultandoIa,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (consultandoIa) stringResource(R.string.btn_thinking) else stringResource(R.string.btn_build))
                    }
                }
                if (avisoIa != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = avisoIa.orEmpty(), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SeccionFormulario(titulo = stringResource(R.string.section_expense_data)) {
                Text(
                    text = stringResource(R.string.label_amount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    placeholder = {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    prefix = {
                        Text(
                            text = "$",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    textStyle = MaterialTheme.typography.displaySmall,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !guardando,
                    colors = coloresCampo(),
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!guardando) expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colorCategoria(categoriaSeleccionada).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconoCategoria(categoriaSeleccionada),
                                contentDescription = null,
                                tint = colorCategoria(categoriaSeleccionada)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_category),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = categoriaSeleccionada.traducida(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        CategoriaGasto.entries.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria.traducida()) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = iconoCategoria(categoria),
                                        contentDescription = null,
                                        tint = colorCategoria(categoria)
                                    )
                                },
                                onClick = {
                                    categoriaNombre = categoria.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text(stringResource(R.string.label_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !guardando,
                    colors = coloresCampo()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !guardando) { mostrarDatePicker = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.label_date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = fechaFormateada,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SeccionFormulario(titulo = stringResource(R.string.section_receipt)) {
                if (vistaPrevia != null) {
                    AsyncImage(
                        model = vistaPrevia,
                        contentDescription = "Foto del ticket",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val permitido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (permitido) {
                                val uri = crearArchivoTicket(context)
                                fotoCamara = uri.toString()
                                camara.launch(uri)
                            } else {
                                permisoCamara.launch(Manifest.permission.CAMERA)
                            }
                        },
                        enabled = !guardando,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_camera))
                    }
                    FilledTonalButton(
                        onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !guardando,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_gallery))
                    }
                }
                if (fotoNueva != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val uri = fotoNueva?.let(Uri::parse) ?: return@Button
                            consultandoIa = true
                            avisoIa = null
                            viewModel.leerTicket(
                                uri = uri,
                                onListo = { sugerencia -> aplicarSugerencia(sugerencia); consultandoIa = false },
                                onError = { mensaje -> avisoIa = mensaje; consultandoIa = false }
                            )
                        },
                        enabled = !guardando && !consultandoIa,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (consultandoIa) stringResource(R.string.loading_reading_ticket) else stringResource(R.string.btn_complete_photo))
                    }
                }
                if (avisoFoto != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = avisoFoto.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val textoLugar = textoUbicacion(lugar, latitud, longitud)

            SeccionFormulario(titulo = stringResource(R.string.section_place)) {
                if (textoLugar != null) {
                    Text(
                        text = textoLugar,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val permitido = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (permitido) tomarUbicacionActual() else permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        enabled = !guardando && !buscandoUbicacion,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (buscandoUbicacion) "Buscando..." else stringResource(R.string.btn_use_location))
                    }

                    if (latitud != null && longitud != null) {
                        FilledTonalButton(
                            onClick = {
                                latitud = null
                                longitud = null
                                lugar = null
                            },
                            enabled = !guardando,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Borrar ubicación")
                        }
                    }
                }

                if (latitud != null && longitud != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        MapaGasto(
                            latitud = latitud!!,
                            longitud = longitud!!,
                            onMover = { nuevaLat, nuevaLon ->
                                latitud = nuevaLat
                                longitud = nuevaLon
                                scope.launch {
                                    etiquetaDeLugar(nuevaLat, nuevaLon)?.let { lugar = it }
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.map_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (avisoUbicacion != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = avisoUbicacion.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            val montoDouble = montoDesdeTexto(amount)

            Button(
                onClick = {
                    if (montoDouble <= 0.0 || guardando) return@Button

                    val foto = fotoNueva?.let(Uri::parse)
                    val fechaSeleccionada = Date(fechaMillis)

                    if (gastoId == null) {
                        viewModel.agregarGasto(
                            amount = montoDouble,
                            category = categoriaSeleccionada,
                            description = description,
                            fotoLocal = foto,
                            latitude = latitud,
                            longitude = longitud,
                            placeLabel = lugar,
                            fecha = fechaSeleccionada,
                            onSuccess = onGuardado
                        )
                    } else {
                        viewModel.actualizarGasto(
                            gastoId = gastoId,
                            amount = montoDouble,
                            category = categoriaSeleccionada,
                            description = description,
                            fotoLocal = foto,
                            latitude = latitud,
                            longitude = longitud,
                            placeLabel = lugar,
                            fecha = fechaSeleccionada,
                            onSuccess = onGuardado
                        )
                    }
                },
                enabled = montoDouble > 0.0 && !guardando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    when {
                        guardando -> stringResource(R.string.btn_saving)
                        editando -> stringResource(R.string.btn_save_changes)
                        else -> stringResource(R.string.btn_save_expense)
                    }
                )
            }

            if (errorGasto != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorGasto.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SeccionFormulario(
    titulo: String,
    color: Color = MaterialTheme.colorScheme.surface,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = color
    ) {
        Column(modifier = Modifier.padding(16.dp), content = contenido)
    }
}

@Composable
private fun coloresCampo() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent
)

private fun montoDesdeTexto(texto: String): Double {
    val limpio = texto.trim().replace(".", "").replace(",", ".")
    return limpio.toDoubleOrNull() ?: 0.0
}

private fun montoParaEditar(amount: Double): String {
    val entero = amount.toLong()
    val centavos = ((amount - entero) * 100).roundToInt()
    return if (centavos == 0) entero.toString() else "%d,%02d".format(entero, centavos)
}