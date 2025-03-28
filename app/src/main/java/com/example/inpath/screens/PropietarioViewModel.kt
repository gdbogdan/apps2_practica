package com.example.inpath.screens

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

data class MascotaInfo(
    val nombre: String,
    val sexo: String,
    val tipo: String,
    var localizacionActivada: Boolean = true
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
}