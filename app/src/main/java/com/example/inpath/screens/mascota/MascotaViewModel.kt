package com.example.inpath.screens.mascota

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.inpath.screens.Posicion
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MascotaViewModel(private val applicationContext: Context) : ViewModel() {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(applicationContext)
    private lateinit var locationCallback: LocationCallback
    private var isLocationTrackingActive = false

    // Propiedades del ViewModel
    private val _posicionMascota = MutableStateFlow(Posicion(0.0, 0.0))
    val posicionMascota: StateFlow<Posicion> = _posicionMascota.asStateFlow()

    private val _mascotaSeleccionada = mutableStateOf<String?>(null)
    val mascotaSeleccionada: String?
        get() = _mascotaSeleccionada.value

    private val _permisosConcedidos = mutableStateOf(false)
    val permisosConcedidos: Boolean
        get() = _permisosConcedidos.value

    private val _mascotasDisponibles = MutableStateFlow<List<String>>(emptyList())
    val mascotasDisponibles: StateFlow<List<String>> = _mascotasDisponibles.asStateFlow()

    fun resetPosicionMascota() {
        _posicionMascota.value = Posicion(0.0, 0.0)
    }

    init {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val newPosicion = Posicion(location.latitude, location.longitude)
                    _posicionMascota.value = newPosicion // Actualiza el StateFlow local

                    val nombre = _mascotaSeleccionada.value
                    val uid = FirebaseAuth.getInstance().currentUser?.uid

                    if (nombre != null && uid != null) {
                        actualizarPosicionMascotaEnFirebase(nombre, newPosicion.latitud, newPosicion.longitud)
                        Log.d("MascotaViewModel", "GPS Real: Subiendo ubicación real de la mascota: ${newPosicion.latitud}, ${newPosicion.longitud}")
                    }
                }
            }
        }
    }

    // --- Métodos de gestión de Fused Location Provider ---
    @SuppressLint("MissingPermission") // Los permisos se gestionan en la Activity
    fun startLocationUpdatesParaMascota() {
        if (isLocationTrackingActive) {
            Log.d("MascotaViewModel", "Las actualizaciones de ubicación de la mascota ya están activas.")
            return
        }

        if (!checkLocationPermissions()) {
            Log.e("MascotaViewModel", "Permisos de ubicación no concedidos. No se pueden iniciar actualizaciones.")
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L) // Actualiza cada 5 segundos
            .setMinUpdateIntervalMillis(2500L) // No más rápido de 2.5 segundos
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
            .addOnSuccessListener {
                isLocationTrackingActive = true
                Log.d("MascotaViewModel", "Actualizaciones de ubicación de la mascota iniciadas.")
            }
            .addOnFailureListener { e ->
                Log.e("MascotaViewModel", "Error al iniciar actualizaciones de ubicación de la mascota: ${e.message}")
            }
    }

    fun stopLocationUpdatesParaMascota() {
        if (!isLocationTrackingActive) {
            Log.d("MascotaViewModel", "Las actualizaciones de ubicación de la mascota no estaban activas.")
            return
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
            .addOnSuccessListener {
                isLocationTrackingActive = false
                Log.d("MascotaViewModel", "Actualizaciones de ubicación de la mascota detenidas.")
            }
            .addOnFailureListener { e ->
                Log.e("MascotaViewModel", "Error al detener actualizaciones de ubicación de la mascota: ${e.message}")
            }
    }

    private fun checkLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // --- Métodos de Firebase ---

    // Función para actualizar la posición de la mascota en Firebase (usada por el callback)
    private fun actualizarPosicionMascotaEnFirebase(nombreMascota: String, latitud: Double, longitud: Double) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val mascotaRef = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid)
            .child("mascotas")
            .child(nombreMascota)

        mascotaRef.child("latitud").setValue(latitud)
        mascotaRef.child("longitud").setValue(longitud)
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

    // --- Otros métodos del ViewModel ---

    fun seleccionarMascota(nombre: String) {
        _mascotaSeleccionada.value = nombre
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid)
            .child("mascotas")
            .child(nombre)
            .child("seleccionada")

        // Marcar la mascota como seleccionada en Firebase
        ref.setValue(true)
        // Iniciar las actualizaciones de ubicación para esta mascota
        startLocationUpdatesParaMascota()
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

    fun verificarPermisosYActualizar(estado: Boolean): Boolean {
        _permisosConcedidos.value = estado
        return estado
    }

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
                    // Importante: No filtres por 'seleccionada' si quieres que el mismo móvil que actúa como GPS también pueda seleccionar la mascota.
                    // La lógica de 'seleccionada' es más para si otro móvil la está 'escuchando'.
                    // Asegúrate de que tu modelo MascotaFirebase contenga estos campos
                    val tipo = mascotaSnap.child("tipo").getValue(String::class.java) // Asegúrate de que existe en MascotaFirebase
                    val sexo = mascotaSnap.child("sexo").getValue(String::class.java) // Asegúrate de que existe en MascotaFirebase

                    if (!nombre.isNullOrBlank() && !tipo.isNullOrBlank() && !sexo.isNullOrBlank()) {
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

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdatesParaMascota() // Asegurarse de detener las actualizaciones al destruir el ViewModel
    }

    // Factory para el ViewModel
    class MascotaViewModelFactory(private val applicationContext: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MascotaViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MascotaViewModel(applicationContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}