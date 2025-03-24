package com.example.inpath.screens

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MascotaViewModel: ViewModel() {
    private val _mascotaSeleccionada = mutableStateOf<String?>(null)
    val mascotaSeleccionada: String?
        get() = _mascotaSeleccionada.value

    fun agregarMascota(nombre: String) {
        if (_mascotaSeleccionada.value == null) {
            _mascotaSeleccionada.value = nombre
        }
    }

    fun eliminarMascota() {
        _mascotaSeleccionada.value = null
    }

    fun obtenerMascotasDisponibles(): List<String> {
        val nombresMascotas = listOf("Killy", "Mango", "Simba", "Niga")
        return nombresMascotas.filter { it != _mascotaSeleccionada.value }
    }
}