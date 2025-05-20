package com.example.inpath.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

data class AreaSegura(
    val nombre: String,
    val latitud: Double,
    val longitud: Double
)

class PropietarioViewModel : ViewModel() {
    private val _areasSeguras = MutableStateFlow(listOf(
        AreaSegura("EPS", 41.60831345075668, 0.6234935707600733),
        AreaSegura("Rectorat", 41.61493849521302, 0.6195960464809468),
        AreaSegura("ETSEA", 41.62781458688859, 0.5961549439884894)
    ))
    val areasSeguras: StateFlow<List<AreaSegura>> = _areasSeguras.asStateFlow()

    /*private val _listaMascotas = MutableStateFlow(mutableListOf<MascotaInfo>())
    val listaMascotas: StateFlow<MutableList<MascotaInfo>> = _listaMascotas.asStateFlow()

    fun agregarMascota(mascota: MascotaInfo) {
        _listaMascotas.update {
            it.apply { add(mascota) }
        }
    }

    fun eliminarMascota(mascota: MascotaInfo) {
        _listaMascotas.update {
            it.apply { remove(mascota) }
        }
    }

    fun actualizarLocalizacion(index: Int, areaSeguraActivada: Boolean) {
        _listaMascotas.value.getOrNull(index)?.areaSegura = areaSeguraActivada
    }*/

    fun agregarAreaSegura(area: AreaSegura) {
        _areasSeguras.update {
            it + area
        }
    }

    fun eliminarAreaSegura(area: AreaSegura) {
        Log.d("PropietarioViewModel", "Eliminando área: ${area.nombre}")
        try {
            _areasSeguras.value = _areasSeguras.value.toMutableList().apply {
                remove(area)
            }
            Log.d("PropietarioViewModel", "Área eliminada correctamente: ${area.nombre}")
        } catch (e: Exception) {
            Log.e("PropietarioViewModel", "Error al eliminar área: ${area.nombre}", e)
        }
    }

    //REALTIME DATABASE
    // Añado StateFlow con las mascotas desde Firebase:
    private val _mascotasFirebase = MutableStateFlow<List<MascotaFirebase>>(emptyList())
    val mascotasFirebase: StateFlow<List<MascotaFirebase>> = _mascotasFirebase.asStateFlow()

    //Dar de alta mascota en FB
    fun subirMascotaAFirebase(
        mascota: MascotaFirebase,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Log.e("PropietarioViewModel", "Usuario no autenticado")
            onFailure(Exception("Usuario no autenticado"))
            return
        }

        val dbRef = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid)
            .child("mascotas")
            .child(mascota.nombre)

        dbRef.setValue(mascota)
            .addOnSuccessListener {
                Log.d("PropietarioViewModel", "Mascota subida correctamente a RTDB")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("PropietarioViewModel", "Error al subir mascota: ${exception.message}")
                onFailure(exception)
            }
    }

    fun observarMascotasDesdeFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid)
            .child("mascotas")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mascotas = mutableListOf<MascotaFirebase>()
                for (mascotaSnapshot in snapshot.children) {
                    val mascota = mascotaSnapshot.getValue(MascotaFirebase::class.java)
                    if ((mascota != null) &&
                        mascota.nombre.isNotBlank() &&
                        mascota.tipo.isNotBlank() &&
                        mascota.sexo.isNotBlank()
                    ) {
                        mascotas.add(mascota)
                    }
                }
                _mascotasFirebase.value = mascotas
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PropietarioViewModel", "Error al leer mascotas: ${error.message}")
            }
        })
    }

    fun actualizarTipoRastreo(nombreMascota: String, tipo: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val dbRef = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid)
            .child("mascotas")
            .child(nombreMascota)
            .child("tipo_rastreo")

        dbRef.setValue(tipo)
    }

    //Dar de baja la mascota en FB:
    fun eliminarMascotaDeFirebase(
        nombreMascota: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            onFailure(Exception("Usuario no autenticado"))
            return
        }

        val dbRef = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid)
            .child("mascotas")
            .child(nombreMascota)

        // Primero la deselecciono
        dbRef.child("seleccionada").setValue(false).addOnCompleteListener {
            // Luego la elimino por completo
            dbRef.removeValue()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { exception -> onFailure(exception) }
        }
    }

    fun observarPosicionMascota(nombreMascota: String, onPosicionActualizada: (Posicion) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid)
            .child("mascotas")
            .child(nombreMascota)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitud").getValue(Double::class.java)
                val lng = snapshot.child("longitud").getValue(Double::class.java)

                if (lat != null && lng != null) {
                    onPosicionActualizada(Posicion(lat, lng))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PropietarioViewModel", "Error leyendo posición: ${error.message}")
            }
        })
    }

}