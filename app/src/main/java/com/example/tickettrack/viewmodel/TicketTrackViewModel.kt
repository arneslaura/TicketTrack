
package com.example.tickettrack.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tickettrack.data.FotosCloudinary
import com.example.tickettrack.data.GeminiCliente
import com.example.tickettrack.data.comprimirTicket
import com.example.tickettrack.model.SugerenciaGasto
import com.example.tickettrack.model.CategoriaGasto
import com.example.tickettrack.model.Gasto
import com.example.tickettrack.notification.HoraRecordatorio
import com.example.tickettrack.notification.Recordatorios
import com.example.tickettrack.ui.theme.Apariencia
import com.example.tickettrack.ui.theme.ModoTema
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class TicketTrackViewModel(application: Application) : AndroidViewModel(application) {

    // Autenticación de Firebase
    private val auth = Firebase.auth

    // Estado del usuario
    private val _usuario = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val usuario: StateFlow<FirebaseUser?> = _usuario

    // Errores de autenticación
    private val _errorAuth = MutableStateFlow<String?>(null)
    val errorAuth: StateFlow<String?> = _errorAuth

    // Base de datos
    private val db = FirebaseFirestore.getInstance()

    private val _guardando = MutableStateFlow(false)
    val guardando = _guardando.asStateFlow()

    private val _errorGasto = MutableStateFlow<String?>(null)
    val errorGasto: StateFlow<String?> = _errorGasto

    // Lista de gastos
    private val _gastos = MutableStateFlow<List<Gasto>>(emptyList())
    val gastos = _gastos.asStateFlow()

    // Recordatorios
    private val _horaRecordatorio =
        MutableStateFlow(Recordatorios.leer(getApplication()))
    val horaRecordatorio: StateFlow<HoraRecordatorio> =
        _horaRecordatorio.asStateFlow()

    // Apariencia
    private val _modoTema =
        MutableStateFlow(Apariencia.leer(getApplication()))
    val modoTema: StateFlow<ModoTema> = _modoTema.asStateFlow()

    init {
        // Mantiene actualizado el estado de la sesión.
        auth.addAuthStateListener { firebaseAuth ->
            _usuario.value = firebaseAuth.currentUser

            if (firebaseAuth.currentUser != null) {
                Recordatorios.programar(getApplication())
            } else {
                Recordatorios.cancelar(getApplication())
            }
        }
    }

    // Registro con email y contraseña
    fun registrar(email: String, pass: String) {
        viewModelScope.launch {
            try {
                _errorAuth.value = null
                auth.createUserWithEmailAndPassword(email, pass).await()
            } catch (e: Exception) {
                _errorAuth.value = mensajeErrorAuth(e)
            }
        }
    }

    // Inicio de sesión con email y contraseña
    fun entrar(email: String, pass: String) {
        viewModelScope.launch {
            try {
                _errorAuth.value = null
                auth.signInWithEmailAndPassword(email, pass).await()
            } catch (e: Exception) {
                _errorAuth.value = mensajeErrorAuth(e)
            }
        }
    }

    fun limpiarErrorAuth() {
        _errorAuth.value = null
    }

    // Cierre de sesión
    fun salir() {
        auth.signOut()

        val opciones = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        ).build()

        GoogleSignIn.getClient(getApplication(), opciones).signOut()
    }

    fun mostrarErrorAuth(mensaje: String) {
        _errorAuth.value = mensaje
    }

    // Inicio de sesión con Google
    fun entrarConGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _errorAuth.value = null

                val credencial =
                    GoogleAuthProvider.getCredential(idToken, null)

                auth.signInWithCredential(credencial).await()
            } catch (e: Exception) {
                _errorAuth.value = mensajeErrorAuth(e)
            }
        }
    }

    // Configuración de recordatorios
    fun cambiarHoraRecordatorio(hora: Int, minuto: Int, activo: Boolean) {
        _horaRecordatorio.value =
            Recordatorios.guardar(getApplication(), hora, minuto, activo)

        Recordatorios.programar(getApplication())
    }

    fun probarRecordatorio() {
        Recordatorios.mostrar(getApplication())
    }

    // Cambiar el tema de la aplicación
    fun cambiarModoTema(modo: ModoTema) {
        _modoTema.value = Apariencia.guardar(getApplication(), modo)
    }

    fun leerTicket(
        uri: Uri,
        onListo: (SugerenciaGasto) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val bytes = comprimirTicket(getApplication(), uri)
                val sugerencia = withContext(Dispatchers.IO) { GeminiCliente.leerTicket(bytes) }
                onListo(sugerencia)
            } catch (e: Exception) {
                onError(e.message ?: "No se pudo leer el ticket")
            }
        }
    }

    fun interpretarFrase(
        frase: String,
        onListo: (SugerenciaGasto) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val sugerencia = withContext(Dispatchers.IO) { GeminiCliente.interpretarFrase(frase) }
                onListo(sugerencia)
            } catch (e: Exception) {
                onError(e.message ?: "No se pudo interpretar el gasto")
            }
        }
    }

    fun preguntar(
        historial: List<Pair<String, String>>,
        onListo: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val respuesta = withContext(Dispatchers.IO) {
                    GeminiCliente.preguntar(_gastos.value, historial)
                }
                onListo(respuesta)
            } catch (e: Exception) {
                onError(e.message ?: "No se pudo consultar al asistente")
            }
        }
    }

    // Obtener los gastos del usuario autenticado
    fun obtenerGastos() {
        val usuarioActual = _usuario.value ?: return

        db.collection("gastos")
            .whereEqualTo("userId", usuarioActual.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val listaGastos = snapshot.documents.mapNotNull { documento ->
                        documento.toObject(Gasto::class.java)
                            ?.copy(id = documento.id)
                    }

                    _gastos.value =
                        listaGastos.sortedByDescending { it.createdAt }
                }
            }
    }

    fun limpiarErrorGasto() {
        _errorGasto.value = null
    }

    // Crear un gasto y subir su foto a Cloudinary si corresponde
    fun agregarGasto(
        amount: Double,
        category: CategoriaGasto,
        description: String,
        fotoLocal: Uri?,
        latitude: Double?,
        longitude: Double?,
        placeLabel: String?,
        fecha: java.util.Date, // <-- ACÁ ENTRA LA FECHA ELEGIDA
        onSuccess: () -> Unit
    ) {
        val usuarioActual = _usuario.value ?: return

        viewModelScope.launch {
            try {
                _guardando.value = true
                _errorGasto.value = null

                val gastoId = UUID.randomUUID().toString()

                val photoUrl = fotoLocal?.let {
                    subirFoto(usuarioActual.uid, gastoId, it)
                }

                val nuevoGasto = Gasto(
                    id = gastoId,
                    userId = usuarioActual.uid,
                    amount = amount,
                    category = category,
                    description = description,
                    photoUrl = photoUrl,
                    latitude = latitude,
                    longitude = longitude,
                    placeLabel = placeLabel?.takeIf { it.isNotBlank() },
                    createdAt = fecha, // <-- ACÁ GUARDAMOS LA FECHA ELEGIDA
                    updatedAt = java.util.Date() // updatedAt sigue siendo el momento exacto actual
                )

                db.collection("gastos")
                    .document(gastoId)
                    .set(nuevoGasto)
                    .await()

                onSuccess()
            } catch (e: Exception) {
                _errorGasto.value = mensajeErrorGasto(e)
            } finally {
                _guardando.value = false
            }
        }
    }

    // Actualizar un gasto y reemplazar su foto si se seleccionó otra
    fun actualizarGasto(
        gastoId: String,
        amount: Double,
        category: CategoriaGasto,
        description: String,
        fotoLocal: Uri?,
        latitude: Double?,
        longitude: Double?,
        placeLabel: String?,
        fecha: java.util.Date, // <-- ACÁ ENTRA LA FECHA ELEGIDA
        onSuccess: () -> Unit
    ) {
        val usuarioActual = _usuario.value ?: return

        val esSuyo = _gastos.value.any {
            it.id == gastoId && it.userId == usuarioActual.uid
        }

        if (!esSuyo) return

        viewModelScope.launch {
            try {
                _guardando.value = true
                _errorGasto.value = null

                val cambios = mutableMapOf<String, Any>(
                    "amount" to amount,
                    "category" to category.name,
                    "description" to description,
                    "createdAt" to fecha, // <-- ACÁ ACTUALIZAMOS LA FECHA
                    "updatedAt" to java.util.Date()
                )

                if (fotoLocal != null) {
                    cambios["photoUrl"] =
                        subirFoto(usuarioActual.uid, gastoId, fotoLocal)
                }

                if (latitude != null && longitude != null) {
                    cambios["latitude"] = latitude
                    cambios["longitude"] = longitude
                    cambios["placeLabel"] = placeLabel.orEmpty()
                }

                db.collection("gastos")
                    .document(gastoId)
                    .update(cambios)
                    .await()

                onSuccess()
            } catch (e: Exception) {
                _errorGasto.value = mensajeErrorGasto(e)
            } finally {
                _guardando.value = false
            }
        }
    }

    // Eliminar la foto de Cloudinary y el gasto de Firestore
    fun eliminarGasto(
        gastoId: String,
        onSuccess: () -> Unit
    ) {
        val usuarioActual = _usuario.value ?: return

        val esSuyo = _gastos.value.any {
            it.id == gastoId && it.userId == usuarioActual.uid
        }

        if (!esSuyo) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    FotosCloudinary.borrar(usuarioActual.uid, gastoId)
                }
            } catch (_: Exception) {
                // Puede no existir una foto o fallar su eliminación.
            }

            try {
                db.collection("gastos")
                    .document(gastoId)
                    .delete()
                    .await()

                onSuccess()
            } catch (e: Exception) {
                _errorGasto.value = mensajeErrorGasto(e)
            }
        }
    }

    // Mensajes de error de autenticación
    private fun mensajeErrorAuth(error: Exception): String {
        val codigo = generarCodigoAuth(error)

        return when (codigo) {
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "Ese email ya está registrado. Ingresá con tu contraseña o con Google."

            "ERROR_INVALID_EMAIL" ->
                "El email no es válido."

            "ERROR_WEAK_PASSWORD" ->
                "La contraseña tiene que tener al menos 6 caracteres."

            "ERROR_WRONG_PASSWORD" ->
                "La contraseña es incorrecta."

            "ERROR_USER_NOT_FOUND" ->
                "No hay una cuenta con ese email. Creá una cuenta nueva."

            "ERROR_USER_DISABLED" ->
                "Esa cuenta está deshabilitada."

            "ERROR_INVALID_CREDENTIAL",
            "ERROR_INVALID_LOGIN_CREDENTIALS" ->
                "El email o la contraseña no coinciden."

            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "Ese email ya está registrado con otro método. Probá con email y contraseña."

            "ERROR_TOO_MANY_REQUESTS" ->
                "Demasiados intentos. Esperá un momento y volvé a probar."

            "ERROR_NETWORK_REQUEST_FAILED" ->
                "No hay conexión. Revisá internet."

            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Ese método de ingreso no está habilitado en Firebase."

            else ->
                "No se pudo completar el ingreso. Probá de nuevo."
        }
    }

    private fun generarCodigoAuth(error: Throwable?): String? {
        var actual = error

        while (actual != null) {
            if (actual is FirebaseAuthException) {
                return actual.errorCode
            }

            actual = actual.cause
        }

        return null
    }

    // Mensajes de error al guardar o actualizar gastos
    private fun mensajeErrorGasto(error: Exception): String {
        val texto = error.localizedMessage
            ?: error.message.orEmpty()

        return when {
            texto.contains("No se pudo subir la foto", ignoreCase = true) ->
                texto

            texto.contains("Falta ", ignoreCase = true) ->
                texto

            texto.contains("does not exist at location", ignoreCase = true) ->
                "No se pudo subir la foto."

            else ->
                texto.ifBlank { "No se pudo guardar el gasto" }
        }
    }

    // Comprimir la foto y subirla a Cloudinary
    private suspend fun subirFoto(
        uid: String,
        gastoId: String,
        uri: Uri
    ): String {
        val bytes = comprimirTicket(getApplication(), uri)

        return withContext(Dispatchers.IO) {
            FotosCloudinary.subir(bytes, uid, gastoId)
        }
    }
}
