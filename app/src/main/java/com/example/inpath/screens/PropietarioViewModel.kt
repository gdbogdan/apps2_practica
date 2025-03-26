package com.example.inpath.screens

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class PropietarioViewModel : ViewModel() {
    private val _listaMascotas = mutableStateListOf<MascotaInfo>()
    val listaMascotas: List<MascotaInfo> = _listaMascotas

    fun agregarMascota(mascota: MascotaInfo) {
        _listaMascotas.add(mascota)
    }

    fun eliminarMascota(mascota: MascotaInfo) {
        _listaMascotas.remove(mascota)
    }
}

data class MascotaInfo(val nombre: String, val sexo: String, val tipo: String)