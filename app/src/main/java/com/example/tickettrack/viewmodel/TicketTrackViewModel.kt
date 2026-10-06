package com.example.tickettrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tickettrack.model.CategoriaGasto
import com.example.tickettrack.model.Gasto
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TicketTrackViewModel : ViewModel() {

    // Conexión con el servicio de Autenticación de Firebase
    private val auth = Firebase.auth

    // Estado del Usuario (Privado para modificar, Público para leer)
    private val _usuario = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val usuario: StateFlow<FirebaseUser?> = _usuario

    // Estado para manejar errores de login/registro
    private val _errorAuth = MutableStateFlow<String?>(null)
    val errorAuth: StateFlow<String?> = _errorAuth

    // Instancia de la base de datos
    private val db = FirebaseFirestore.getInstance()

    // flujo de datos para la lista de gastos
    private val _gastos = MutableStateFlow<List<Gasto>>(emptyList<Gasto>())
    val gastos = _gastos.asStateFlow()

    init {
        // Oyente de la sesión
        auth.addAuthStateListener { firebaseAuth ->
            _usuario.value = firebaseAuth.currentUser
        }
    }

    // Funciones para Eventos
    fun registrar(email: String, pass: String) {
        viewModelScope.launch {
            try {
                _errorAuth.value = null
                auth.createUserWithEmailAndPassword(email, pass).await()
            } catch (e: Exception) {
                _errorAuth.value = e.localizedMessage
            }
        }
    }

    fun entrar(email: String, pass: String) {
        viewModelScope.launch {
            try {
                _errorAuth.value = null
                auth.signInWithEmailAndPassword(email, pass).await()
            } catch (e: Exception) {
                _errorAuth.value = e.localizedMessage
            }
        }
    }

    fun salir() {
        auth.signOut()
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

    // Función para crear un nuevo gasto en Firestore
    fun agregarGasto(
        amount: Double,
        category: CategoriaGasto,
        description: String,
        onSuccess: () -> Unit
    ) {
        val usuarioActual = _usuario.value ?: return

        val nuevoGasto = Gasto(
            userId = usuarioActual.uid,
            amount = amount,
            category = category,
            description = description,
            createdAt = java.util.Date()
        )

        db.collection("gastos")
            .add(nuevoGasto)
            .addOnSuccessListener {
                // Si se guarda bien, ejecutamos el callback para volver a la pantalla anterior
                onSuccess()
            }
            .addOnFailureListener {
            }
    }
}