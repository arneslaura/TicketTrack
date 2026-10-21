
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
    var categoriaNombre by rememberSaveable {
        mutableStateOf(CategoriaGasto.OTROS.name)
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var formularioListo by rememberSaveable {
        mutableStateOf(!editando)
    }

    // Fecha del gasto
    var fechaMillis by rememberSaveable {
        mutableLongStateOf(System.currentTimeMillis())
    }
    var mostrarDatePicker by rememberSaveable {
        mutableStateOf(false)
    }

    // Fotografía del comprobante
    var fotoActualUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var fotoNueva by rememberSaveable { mutableStateOf<String?>(null) }
    var fotoCamara by rememberSaveable { mutableStateOf<String?>(null) }
    var avisoFoto by rememberSaveable { mutableStateOf<String?>(null) }

    // Ubicación
    var latitud by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitud by rememberSaveable { mutableStateOf<Double?>(null) }
    var lugar by rememberSaveable { mutableStateOf<String?>(null) }
    var buscandoUbicacion by rememberSaveable { mutableStateOf(false) }
    var avisoUbicacion by rememberSaveable { mutableStateOf<String?>(null) }

    // Gemini y reconocimiento de voz
    var frase by rememberSaveable { mutableStateOf("") }
    var consultandoIa by rememberSaveable { mutableStateOf(false) }
    var avisoIa by rememberSaveable { mutableStateOf<String?>(null) }

    val fused = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val scope = rememberCoroutineScope()

    fun tomarUbicacionActual() {
        scope.launch {
            buscandoUbicacion = true
            avisoUbicacion = null

            try {
                val actual = fused.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await() ?: fused.lastLocation.await()

                if (actual == null) {
                    avisoUbicacion =
                        "No se obtuvo una ubicación. Activá el GPS del teléfono o del emulador."
                } else {
                    latitud = actual.latitude
                    longitud = actual.longitude
                    lugar = etiquetaDeLugar(
                        context,
                        actual.latitude,
                        actual.longitude
                    )
                }
            } catch (_: SecurityException) {
                avisoUbicacion = "Falta el permiso de ubicación."
            } catch (e: Exception) {
                avisoUbicacion =
                    e.localizedMessage ?: "No se pudo obtener la ubicación."
            } finally {
                buscandoUbicacion = false
            }
        }
    }

    val permisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            tomarUbicacionActual()
        } else {
            avisoUbicacion =
                "Para guardar el lugar, aceptá el permiso de ubicación."
        }
    }

    val categoriaSeleccionada = CategoriaGasto.entries
        .find { it.name == categoriaNombre }
        ?: CategoriaGasto.OTROS

    val vistaPrevia = fotoNueva ?: fotoActualUrl

    // Cámara
    val camara = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            fotoNueva = fotoCamara
            avisoFoto = null
        }
    }

    val permisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = crearArchivoTicket(context)
            fotoCamara = uri.toString()
            camara.launch(uri)
        } else {
            avisoFoto = "Para usar la cámara, aceptá el permiso."
        }
    }

    // Reconocimiento de voz
    val voz = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val textos = resultado.data?.getStringArrayListExtra(
            RecognizerIntent.EXTRA_RESULTS
        )

        if (!textos.isNullOrEmpty()) {
            frase = textos.first()
            avisoIa = null
        }
    }

    fun iniciarDictado() {
        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Contá el gasto"
            )
        }

        try {
            voz.launch(intent)
        } catch (_: ActivityNotFoundException) {
            avisoIa =
                "Este dispositivo no reconoce la voz. Escribí el gasto."
        }
    }

    val permisoMicrofono = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            iniciarDictado()
        } else {
            avisoIa =
                "Para dictar el gasto, aceptá el permiso del micrófono."
        }
    }

    // Aplicar la sugerencia generada por Gemini
    fun aplicarSugerencia(sugerencia: SugerenciaGasto) {
        sugerencia.amount?.let {
            amount = montoParaEditar(it)
        }

        sugerencia.category?.let {
            categoriaNombre = it.name
        }

        if (sugerencia.description.isNotBlank()) {
            description = sugerencia.description
        }

        if (!sugerencia.placeLabel.isNullOrBlank()) {
            lugar = sugerencia.placeLabel
        }

        avisoIa = "Revisá los datos antes de guardar."
    }

    // Galería
    val galeria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            fotoNueva = uri.toString()
            avisoFoto = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.limpiarErrorGasto()
    }

    // Cargar los datos existentes al editar un gasto
    LaunchedEffect(gastoId, gastos) {
        if (formularioListo || gastoId == null) {
            return@LaunchedEffect
        }

        val gasto = gastos.find { it.id == gastoId }
            ?: return@LaunchedEffect

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {

            TextButton(
                onClick = onVolver,
                enabled = !guardando
            ) {
                Text(
                    text = "← Volver",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (editando) "Editar gasto" else "Nuevo gasto",
                style = MaterialTheme.typography.headlineMedium
            )

            if (editando && !formularioListo) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Cargando gasto...")
                return@Column
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Selector de fecha
            val fechaFormateada = remember(fechaMillis) {
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).format(Date(fechaMillis))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!guardando) {
                            mostrarDatePicker = true
                        }
                    }
            ) {
                OutlinedTextField(
                    value = fechaFormateada,
                    onValueChange = {},
                    label = { Text("Fecha") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor =
                            MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor =
                            MaterialTheme.colorScheme.outline,
                        disabledLabelColor =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            if (mostrarDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = fechaMillis
                )

                DatePickerDialog(
                    onDismissRequest = {
                        mostrarDatePicker = false
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    fechaMillis = it
                                }
                                mostrarDatePicker = false
                            }
                        ) {
                            Text("Aceptar")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                mostrarDatePicker = false
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Entrada de gastos mediante Gemini
            Text(
                text = "Contalo con Gemini",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = frase,
                onValueChange = { frase = it },
                label = {
                    Text("Ejemplo: gasté 4500 en el súper")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando && !consultandoIa
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val permitido = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (permitido) {
                            iniciarDictado()
                        } else {
                            permisoMicrofono.launch(
                                Manifest.permission.RECORD_AUDIO
                            )
                        }
                    },
                    enabled = !guardando && !consultandoIa,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dictar")
                }

                Button(
                    onClick = {
                        val texto = frase.trim()
                        if (texto.isEmpty()) return@Button

                        consultandoIa = true
                        avisoIa = null

                        viewModel.interpretarFrase(
                            frase = texto,
                            onListo = { sugerencia ->
                                aplicarSugerencia(sugerencia)
                                consultandoIa = false
                            },
                            onError = { mensaje ->
                                avisoIa = mensaje
                                consultandoIa = false
                            }
                        )
                    },
                    enabled = frase.isNotBlank() &&
                            !guardando &&
                            !consultandoIa,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (consultandoIa) {
                            "Pensando..."
                        } else {
                            "Armar gasto"
                        }
                    )
                }
            }

            if (avisoIa != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = avisoIa.orEmpty(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Monto
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Monto ($)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categorías
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    if (!guardando) {
                        expanded = !expanded
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = categoriaSeleccionada.descripcion,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    CategoriaGasto.entries.forEach { categoria ->
                        DropdownMenuItem(
                            text = {
                                Text(categoria.descripcion)
                            },
                            onClick = {
                                categoriaNombre = categoria.name
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Descripción
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Comprobante
            Text(
                text = "Comprobante",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (vistaPrevia != null) {
                AsyncImage(
                    model = vistaPrevia,
                    contentDescription = "Foto del ticket",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val permitido = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (permitido) {
                            val uri = crearArchivoTicket(context)
                            fotoCamara = uri.toString()
                            camara.launch(uri)
                        } else {
                            permisoCamara.launch(
                                Manifest.permission.CAMERA
                            )
                        }
                    },
                    enabled = !guardando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cámara")
                }

                OutlinedButton(
                    onClick = {
                        galeria.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    enabled = !guardando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Galería")
                }
            }

            if (fotoNueva != null) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val uri = fotoNueva?.let(Uri::parse)
                            ?: return@OutlinedButton

                        consultandoIa = true
                        avisoIa = null

                        viewModel.leerTicket(
                            uri = uri,
                            onListo = { sugerencia ->
                                aplicarSugerencia(sugerencia)
                                consultandoIa = false
                            },
                            onError = { mensaje ->
                                avisoIa = mensaje
                                consultandoIa = false
                            }
                        )
                    },
                    enabled = !guardando && !consultandoIa,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (consultandoIa) {
                            "Leyendo ticket..."
                        } else {
                            "Completar con la foto"
                        }
                    )
                }
            }

            if (avisoFoto != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = avisoFoto.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Ubicación
            Text(
                text = "Lugar",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            val textoLugar = textoUbicacion(
                lugar,
                latitud,
                longitud
            )

            Text(
                text = textoLugar ?: "Todavía sin ubicación",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val permitido = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (permitido) {
                        tomarUbicacionActual()
                    } else {
                        permisoUbicacion.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    }
                },
                enabled = !guardando && !buscandoUbicacion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (buscandoUbicacion) {
                        "Buscando ubicación..."
                    } else {
                        "Usar mi ubicación"
                    }
                )
            }

            if (latitud != null && longitud != null) {
                Spacer(modifier = Modifier.height(12.dp))

                MapaGasto(
                    latitud = latitud!!,
                    longitud = longitud!!,
                    onMover = { nuevaLat, nuevaLon ->
                        latitud = nuevaLat
                        longitud = nuevaLon

                        scope.launch {
                            etiquetaDeLugar(
                                context,
                                nuevaLat,
                                nuevaLon
                            )?.let {
                                lugar = it
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Tocá el mapa para ajustar el punto",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (avisoUbicacion != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = avisoUbicacion.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Guardar o actualizar el gasto
            val montoDouble = montoDesdeTexto(amount)

            Button(
                onClick = {
                    if (montoDouble <= 0.0 || guardando) {
                        return@Button
                    }

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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        guardando -> "Guardando..."
                        editando -> "Guardar cambios"
                        else -> "Guardar gasto"
                    }
                )
            }

            if (errorGasto != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorGasto.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun montoDesdeTexto(texto: String): Double {
    val limpio = texto.trim()
        .replace(".", "")
        .replace(",", ".")

    return limpio.toDoubleOrNull() ?: 0.0
}

private fun montoParaEditar(amount: Double): String {
    val entero = amount.toLong()
    val centavos = ((amount - entero) * 100).roundToInt()

    return if (centavos == 0) {
        entero.toString()
    } else {
        "%d,%02d".format(entero, centavos)
    }
}
