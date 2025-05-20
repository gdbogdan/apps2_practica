package com.example.inpath.screens

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MascotaViewModel : ViewModel() {

    private val _posicionMascota = MutableStateFlow(Posicion(0.0, 0.0))
    val posicionMascota: StateFlow<Posicion> = _posicionMascota.asStateFlow()

    private val _mascotaSeleccionada = mutableStateOf<String?>(null)
    val mascotaSeleccionada: String?
        get() = _mascotaSeleccionada.value

    fun seleccionarMascota(nombre: String) {
        _mascotaSeleccionada.value = nombre
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid)
            .child("mascotas")
            .child(nombre)
            .child("seleccionada")

        ref.setValue(true)
    }

    fun eliminarMascota() {
        val nombre = _mascotaSeleccionada.value ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        _mascotaSeleccionada.value = null
        _permisosConcedidos.value = false

        val refMascota = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid)
            .child("mascotas")
            .child(nombre)

        // Comprueba si la mascota aún existe antes de modificar "seleccionada"
        refMascota.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    refMascota.child("seleccionada").setValue(false)
                } else {
                    Log.w("MascotaViewModel", "Mascota ya no existe en RTDB. No se marca como deseleccionada.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MascotaViewModel", "Error comprobando existencia de mascota: ${error.message}")
            }
        })
    }

    private val _permisosConcedidos = mutableStateOf(false)
    val permisosConcedidos: Boolean
        get() = _permisosConcedidos.value

    fun verificarPermisosYActualizar(estado: Boolean): Boolean {
        _permisosConcedidos.value = estado
        return estado
    }

    // RTDB - Obtener lista de mascotas registradas
    private val _mascotasDisponibles = MutableStateFlow<List<String>>(emptyList())
    val mascotasDisponibles: StateFlow<List<String>> = _mascotasDisponibles.asStateFlow()

    fun cargarMascotasDesdeFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid)
            .child("mascotas")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombres = mutableListOf<String>()
                for (mascotaSnap in snapshot.children) {
                    val nombre = mascotaSnap.child("nombre").getValue(String::class.java)
                    val seleccionada = mascotaSnap.child("seleccionada").getValue(Boolean::class.java) ?: false
                    val tipo = mascotaSnap.child("tipo").getValue(String::class.java)
                    val sexo = mascotaSnap.child("sexo").getValue(String::class.java)

                    // Solo añadimos si tiene datos válidos y no está seleccionada por otro móvil
                    if (!seleccionada && !nombre.isNullOrBlank() && !tipo.isNullOrBlank() && !sexo.isNullOrBlank()) {
                        nombres.add(nombre)
                    }
                }
                _mascotasDisponibles.value = nombres
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MascotaViewModel", "Error cargando mascotas: ${error.message}")
            }
        })
    }


    fun iniciarActualizacionPosicion() {
        val nombre = _mascotaSeleccionada.value ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            var posicion = Posicion(
                latitud = 41.60831345075668, // Se puede reemplazar por Ubicación Actual, pero en emuladores no se puede hacer
                longitud = 0.6234935707600733
            )

            val ref = FirebaseDatabase.getInstance().reference
                .child("usuarios")
                .child(uid)
                .child("mascotas")
                .child(nombre)

            while (true) {
                // Comprobar si sigue seleccionada
                val seleccionadaSnapshot = ref.child("seleccionada").get().await()
                val seleccionada = seleccionadaSnapshot.getValue(Boolean::class.java) ?: false

                if (!seleccionada) break

                posicion = simularMovimiento(posicion)

                val dbRef = FirebaseDatabase.getInstance().reference
                    .child("usuarios")
                    .child(uid)
                    .child("mascotas")
                    .child(nombre)

                dbRef.child("latitud").setValue(posicion.latitud)
                dbRef.child("longitud").setValue(posicion.longitud)

                delay(10000)
            }
        }
    }

    private fun simularMovimiento(posicionActual: Posicion): Posicion {
        val velocidad = Random.nextDouble(0.0001, 0.00030)
        val angulo = Random.nextDouble(0.0, 2 * Math.PI)

        val nuevaLatitud = posicionActual.latitud + velocidad * cos(angulo)
        val nuevaLongitud = posicionActual.longitud + velocidad * sin(angulo)

        return Posicion(nuevaLatitud, nuevaLongitud)
    }

    fun observarPosicionDesdeFirebase() {
        val nombre = _mascotaSeleccionada.value ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid)
            .child("mascotas")
            .child(nombre)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitud").getValue(Double::class.java)
                val lng = snapshot.child("longitud").getValue(Double::class.java)
                if (lat != null && lng != null) {
                    _posicionMascota.value = Posicion(lat, lng)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MascotaViewModel", "Error al leer posición: ${error.message}")
            }
        })
    }
}
