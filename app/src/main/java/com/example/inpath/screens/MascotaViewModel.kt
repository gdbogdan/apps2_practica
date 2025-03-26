package com.example.inpath.screens

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MascotaViewModel : ViewModel() {
    private val _mascotaSeleccionada = mutableStateOf<String?>(null)
    val mascotaSeleccionada: String?
        get() = _mascotaSeleccionada.value

    private val _permisosConcedidos = mutableStateOf(false)
    val permisosConcedidos: Boolean
        get() = _permisosConcedidos.value

    fun agregarMascota(nombre: String) {
        if (_mascotaSeleccionada.value == null && _permisosConcedidos.value) {
            _mascotaSeleccionada.value = nombre
        }
    }

    fun eliminarMascota() {
        _mascotaSeleccionada.value = null
        _permisosConcedidos.value = false
    }

    fun obtenerMascotasDisponibles(): List<String> {
        val nombresMascotas = listOf("Kili", "Mango", "Simba", "Nigga")
        return nombresMascotas.filter { it != _mascotaSeleccionada.value }
    }

    fun verificarPermisosYActualizar(estado: Boolean): Boolean {
        _permisosConcedidos.value = estado
        return estado
    }
}
