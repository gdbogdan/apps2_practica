package com.example.inpath.screens.propietario

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.inpath.screens.mascota.MascotaFirebase
import com.example.inpath.screens.Posicion
import com.example.inpath.screens.geofence.AreaSegura
import com.example.inpath.screens.geofence.GeoFenceManager
import com.example.inpath.screens.geofence.GeofenceBroadcastReceiver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PropietarioViewModel() : ViewModel() {

    private val firebaseRef = FirebaseDatabase.getInstance().reference

    // --- Áreas Seguras ---
    private val _areasSeguras = MutableStateFlow<List<AreaSegura>>(emptyList())
    val areasSeguras: StateFlow<List<AreaSegura>> = _areasSeguras.asStateFlow()

    private var geoFenceManager: GeoFenceManager? = null

    fun setGeoFenceManager(manager: GeoFenceManager) {
        geoFenceManager = manager
    }

    fun agregarAreaSegura(area: AreaSegura) {
        _areasSeguras.update { it + area }
        guardarAreaEnRTDB(area)
    }

    fun actualizarAreaSegura(areaAntigua: AreaSegura, areaNueva: AreaSegura) {
        _areasSeguras.update {
            it.map { existente -> if (existente.nombre == areaAntigua.nombre) areaNueva else existente }
        }
        guardarAreaEnRTDB(areaNueva)
    }

    fun eliminarAreaSegura(area: AreaSegura) {
        _areasSeguras.update { it.filterNot { it.nombre == area.nombre } }
        eliminarAreaDeRTDB(area)
    }

    private fun guardarAreaEnRTDB(area: AreaSegura) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firebaseRef.child("usuarios").child(uid).child("areas_seguras").child(area.nombre).setValue(area)
    }

    private fun eliminarAreaDeRTDB(area: AreaSegura) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firebaseRef.child("usuarios").child(uid).child("areas_seguras").child(area.nombre).removeValue()
    }

    // --- Snackbar desde Broadcast (del GeofenceBroadcastReceiver) ---
    private val _geofenceSnackbarEvents = MutableStateFlow<String?>(null)
    val geofenceSnackbarEvents: StateFlow<String?> = _geofenceSnackbarEvents.asStateFlow()

    private val geofenceEventReceiver = object : BroadcastReceiver() { // ¡Reincorporado!
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == GeofenceBroadcastReceiver.Companion.ACTION_GEOFENCE_TRANSITION_EVENT) {
                val message = intent.getStringExtra("message")
                message?.let {
                    _geofenceSnackbarEvents.value = it
                }
            }
        }
    }

    fun emitirGeofenceSnackbar(mensaje: String) {
        _geofenceSnackbarEvents.value = mensaje
    }

    fun consumeGeofenceSnackbarEvent() {
        _geofenceSnackbarEvents.value = null
    }

    // --- Mascotas en Firebase ---
    private val _mascotasFirebase = MutableStateFlow<List<MascotaFirebase>>(emptyList())
    val mascotasFirebase: StateFlow<List<MascotaFirebase>> = _mascotasFirebase.asStateFlow()

    // Para la posición de la mascota que el propietario está visualizando en el mapa
    private val _posicionMascotaMostrada = MutableStateFlow(Posicion(0.0, 0.0))
    val posicionMascotaMostrada: StateFlow<Posicion> = _posicionMascotaMostrada.asStateFlow()

    private var currentPosicionListener: ValueEventListener? = null // Para gestionar el listener de posición

    fun subirMascotaAFirebase(mascota: MascotaFirebase, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onFailure(Exception("Usuario no autenticado"))

        firebaseRef.child("usuarios").child(uid).child("mascotas").child(mascota.nombre)
            .setValue(mascota)
            .addOnSuccessListener {
                Log.d("PropietarioViewModel", "Mascota subida correctamente a RTDB")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("PropietarioViewModel", "Error al subir mascota: ${it.message}")
                onFailure(it)
            }
    }

    fun observarMascotasDesdeFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firebaseRef.child("usuarios").child(uid).child("mascotas")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mascotas = snapshot.children.mapNotNull { it.getValue(MascotaFirebase::class.java) }
                        .filter { it.nombre.isNotBlank() && it.tipo.isNotBlank() && it.sexo.isNotBlank() }
                    _mascotasFirebase.value = mascotas
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PropietarioViewModel", "Error al leer mascotas: ${error.message}")
                }
            })
    }

    fun actualizarTipoRastreo(nombreMascota: String, tipo: String, areaSegura: AreaSegura? = null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val mascotaRef = firebaseRef.child("usuarios").child(uid).child("mascotas").child(nombreMascota)

        mascotaRef.child("tipoRastreo").setValue(tipo)

        // ¡CAMBIO CLAVE! Actualiza el campo 'tieneGeofence'
        val tieneGeofenceAhora = (tipo == "AreaSegura" && areaSegura != null)
        mascotaRef.child("tieneGeofence").setValue(tieneGeofenceAhora)

        if (tipo == "AreaSegura" && areaSegura != null) {
            mascotaRef.child("nombreArea").setValue(areaSegura.nombre)
            mascotaRef.child("areaSeguraLatitud").setValue(areaSegura.latitud)
            mascotaRef.child("areaSeguraLongitud").setValue(areaSegura.longitud)
            mascotaRef.child("areaSeguraRadio").setValue(areaSegura.radio)

            geoFenceManager?.agregarGeofence(nombreMascota, areaSegura.latitud, areaSegura.longitud, areaSegura.radio)
        } else {
            geoFenceManager?.eliminarGeofence(nombreMascota)
            mascotaRef.child("nombreArea").removeValue()
            mascotaRef.child("areaSeguraLatitud").removeValue()
            mascotaRef.child("areaSeguraLongitud").removeValue()
            mascotaRef.child("areaSeguraRadio").removeValue()
        }
    }

    fun eliminarMascotaDeFirebase(nombreMascota: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onFailure(Exception("Usuario no autenticado"))
        val mascotaRef = firebaseRef.child("usuarios").child(uid).child("mascotas").child(nombreMascota)

        geoFenceManager?.eliminarGeofence(nombreMascota)

        mascotaRef.child("seleccionada").setValue(false).addOnCompleteListener {
            mascotaRef.removeValue()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        }
    }

    fun observarPosicionParaMapa(nombreMascota: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Si ya hay un listener activo, lo removemos para evitar múltiples escuchas
        currentPosicionListener?.let {
            firebaseRef.child("usuarios").child(uid).child("mascotas").child(nombreMascota).removeEventListener(it)
        }

        val ref = firebaseRef
            .child("usuarios")
            .child(uid)
            .child("mascotas")
            .child(nombreMascota)

        currentPosicionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitud").getValue(Double::class.java)
                val lng = snapshot.child("longitud").getValue(Double::class.java)

                if (lat != null && lng != null) {
                    _posicionMascotaMostrada.value = Posicion(lat, lng) // Actualiza el StateFlow
                    Log.d("PropietarioViewModel", "Posición para mapa actualizada: $lat, $lng")
                } else {
                    Log.d("PropietarioViewModel", "No se encontró lat/lng para ${nombreMascota}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PropietarioViewModel", "Error leyendo posición para mapa: ${error.message}")
            }
        }
        ref.addValueEventListener(currentPosicionListener!!)
    }

    fun observarAreasSegurasDesdeFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firebaseRef.child("usuarios").child(uid).child("areas_seguras")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val areas = snapshot.children.mapNotNull { it.getValue(AreaSegura::class.java) }
                    _areasSeguras.value = areas
                    Log.d("PropietarioViewModel", "Áreas seguras cargadas: ${areas.size}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PropietarioViewModel", "Error al leer áreas seguras: ${error.message}")
                }
            })
    }

    // Método para detener la observación de la posición en el mapa
    fun detenerObservacionPosicionMapa() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        currentPosicionListener?.let { listener ->
            _posicionMascotaMostrada.value = Posicion(0.0, 0.0) // Resetea la posición en la UI
            Log.d("PropietarioViewModel", "Observación de posición para mapa detenida.")
        }
    }
}