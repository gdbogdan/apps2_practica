package com.example.inpath.screens

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

data class MascotaInfo(
    val nombre: String,
    val sexo: String,
    val tipo: String,
    var localizacionActivada: Boolean = true
)

data class AreaSegura(
    val nombre: String,
    val latitud: Double,
    val longitud: Double
)

class PropietarioViewModel : ViewModel() {
    var listaMascotas = mutableStateListOf<MascotaInfo>()
        private set

    fun agregarMascota(mascota: MascotaInfo) {
        listaMascotas.add(mascota)
    }

    fun eliminarMascota(mascota: MascotaInfo) {
        listaMascotas.remove(mascota)
    }

    fun actualizarLocalizacion(index: Int, activada: Boolean) {
        if (index in 0 until listaMascotas.size) {
            listaMascotas[index] = listaMascotas[index].copy(localizacionActivada = activada)
        }
    }

    val areasSeguras = listOf(
        AreaSegura("EPS", 41.60831345075668, 0.6234935707600733),
        AreaSegura("Rectorat", 41.61493849521302, 0.6195960464809468),
        AreaSegura("ETSEA", 41.62781458688859, 0.5961549439884894)
    )
}