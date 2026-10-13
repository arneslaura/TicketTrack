package com.example.tickettrack.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tickettrack.data.comprimirTicket
import com.example.tickettrack.model.CategoriaGasto
import com.example.tickettrack.model.Gasto
import com.example.tickettrack.notification.HoraRecordatorio
import com.example.tickettrack.notification.Recordatorios
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TicketTrackViewModel(application: Application) : AndroidViewModel(application) {

    // Conexión con el servicio de Autenticación de Firebase
    private val auth = Firebase.auth

    // Estado del Usuario (Privado para modificar, Público para leer)
    private val _usuario = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val usuario: StateFlow<FirebaseUser?> = _usuario

    // Estado para manejar errores de login/registro
    private val _errorAuth = MutableStateFlow<String?>(null)
    val errorAuth: StateFlow<String?> = _errorAuth

    // Instancia de la base de datos y de las fotos
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _guardando = MutableStateFlow(false)
    val guardando = _guardando.asStateFlow()

    private val _errorGasto = MutableStateFlow<String?>(null)
    val errorGasto: StateFlow<String?> = _errorGasto

    // flujo de datos para la lista de gastos
    private val _gastos = MutableStateFlow<List<Gasto>>(emptyList<Gasto>())
    val gastos = _gastos.asStateFlow()

    private val _horaRecordatorio = MutableStateFlow(Recordatorios.leer(getApplication()))
    val horaRecordatorio: StateFlow<HoraRecordatorio> = _horaRecordatorio.asStateFlow()

    init {
        // Oyente de la sesión. Con usuario logueado queda agendado el recordatorio.
        auth.addAuthStateListener { firebaseAuth ->
            _usuario.value = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                Recordatorios.programar(getApplication())
            } else {
                Recordatorios.cancelar(getApplication())
            }
        }
    }

    // Funciones para Eventos
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

    fun salir() {
        auth.signOut()
        val opciones = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(getApplication(), opciones).signOut()
    }

    fun mostrarErrorAuth(mensaje: String) {
        _errorAuth.value = mensaje
    }

    fun entrarConGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _errorAuth.value = null
                val credencial = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credencial).await()
            } catch (e: Exception) {
                _errorAuth.value = mensajeErrorAuth(e)
            }
        }
    }

    fun cambiarHoraRecordatorio(hora: Int, minuto: Int) {
        _horaRecordatorio.value = Recordatorios.guardar(getApplication(), hora, minuto)
        Recordatorios.programar(getApplication())
    }

    fun probarRecordatorio() {
        Recordatorios.mostrar(getApplication())
    }

    // Función para obtener los gastos
    fun obtenerGastos() {
        val usuarioActual = _usuario.value ?: return

        // colección gastos que le pertenecen a ese usuario
        db.collection("gastos")
            .whereEqualTo("userId", usuarioActual.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // mapeo de  documentos a clase Gasto
                    val listaGastos = snapshot.documents.mapNotNull { documento ->
                        documento.toObject(Gasto::class.java)?.copy(id = documento.id)
                    }

                    //  lista ordenada por fecha
                    _gastos.value = listaGastos.sortedByDescending { it.createdAt }
                }
            }
    }

    fun limpiarErrorGasto() {
        _errorGasto.value = null
    }

    // Crea el gasto. Si hay foto, primero la sube y guarda la URL en el documento.
    fun agregarGasto(
        amount: Double,
        category: CategoriaGasto,
        description: String,
        fotoLocal: Uri?,
        latitude: Double?,
        longitude: Double?,
        placeLabel: String?,
        onSuccess: () -> Unit
    ) {
        val usuarioActual = _usuario.value ?: return

        viewModelScope.launch {
            try {
                _guardando.value = true
                _errorGasto.value = null
                val gastoId = UUID.randomUUID().toString()
                val photoUrl = fotoLocal?.let { subirFoto(usuarioActual.uid, gastoId, it) }
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
                    createdAt = java.util.Date(),
                    updatedAt = java.util.Date()
                )
                db.collection("gastos").document(gastoId).set(nuevoGasto).await()
                onSuccess()
            } catch (e: Exception) {
                _errorGasto.value = mensajeErrorGasto(e)
            } finally {
                _guardando.value = false
            }
        }
    }

    // Actualiza monto, categoría y nota. Si eligieron otra foto, reemplaza la anterior.
    fun actualizarGasto(
        gastoId: String,
        amount: Double,
        category: CategoriaGasto,
        description: String,
        fotoLocal: Uri?,
        latitude: Double?,
        longitude: Double?,
        placeLabel: String?,
        onSuccess: () -> Unit
    ) {
        val usuarioActual = _usuario.value ?: return
        val esSuyo = _gastos.value.any { it.id == gastoId && it.userId == usuarioActual.uid }
        if (!esSuyo) return

        viewModelScope.launch {
            try {
                _guardando.value = true
                _errorGasto.value = null
                val cambios = mutableMapOf<String, Any>(
                    "amount" to amount,
                    "category" to category.name,
                    "description" to description,
                    "updatedAt" to java.util.Date()
                )
                if (fotoLocal != null) {
                    cambios["photoUrl"] = subirFoto(usuarioActual.uid, gastoId, fotoLocal)
                }
                if (latitude != null && longitude != null) {
                    cambios["latitude"] = latitude
                    cambios["longitude"] = longitude
                    cambios["placeLabel"] = placeLabel.orEmpty()
                }
                db.collection("gastos").document(gastoId).update(cambios).await()
                onSuccess()
            } catch (e: Exception) {
                _errorGasto.value = mensajeErrorGasto(e)
            } finally {
                _guardando.value = false
            }
        }
    }

    // Borra el documento y, si había, el archivo de la foto.
    fun eliminarGasto(gastoId: String, onSuccess: () -> Unit) {
        val usuarioActual = _usuario.value ?: return
        val esSuyo = _gastos.value.any { it.id == gastoId && it.userId == usuarioActual.uid }
        if (!esSuyo) return

        viewModelScope.launch {
            try {
                storage.reference
                    .child("users/${usuarioActual.uid}/tickets/$gastoId.jpg")
                    .delete()
                    .await()
            } catch (_: Exception) {
                // El gasto puede no tener foto.
            }

            db.collection("gastos")
                .document(gastoId)
                .delete()
                .addOnSuccessListener { onSuccess() }
        }
    }

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
            "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" ->
                "El email o la contraseña no coinciden."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "Ese email ya está registrado con otro método. Probá con email y contraseña."
            "ERROR_TOO_MANY_REQUESTS" ->
                "Demasiados intentos. Esperá un momento y volvé a probar."
            "ERROR_NETWORK_REQUEST_FAILED" ->
                "No hay conexión. Revisá internet."
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Ese método de ingreso no está habilitado en Firebase."
            else -> "No se pudo completar el ingreso. Probá de nuevo."
        }
    }

    private fun generarCodigoAuth(error: Throwable?): String? {
        var actual = error
        while (actual != null) {
            if (actual is FirebaseAuthException) return actual.errorCode
            actual = actual.cause
        }
        return null
    }

    private fun mensajeErrorGasto(error: Exception): String {
        val texto = error.localizedMessage.orEmpty()
        return if (texto.contains("does not exist at location", ignoreCase = true)) {
            "No se pudo subir la foto. Creá Storage en Firebase y publicá las reglas de storage.rules."
        } else {
            texto.ifBlank { "No se pudo guardar el gasto" }
        }
    }

    private suspend fun subirFoto(uid: String, gastoId: String, uri: Uri): String {
        val bytes = comprimirTicket(getApplication(), uri)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        val ref = storage.reference.child("users/$uid/tickets/$gastoId.jpg")
        ref.putBytes(bytes, metadata).await()
        return ref.downloadUrl.await().toString()
    }
}