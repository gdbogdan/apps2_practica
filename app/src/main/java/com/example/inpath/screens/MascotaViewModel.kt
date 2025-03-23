package com.example.inpath.screens

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class MascotaViewModel: ViewModel() {
    private val _mascotas = mutableStateListOf<String>()
    val mascotas: List<String> = _mascotas

    fun agregarMascota(nombre: String) {
        if (!_mascotas.contains(nombre)) {
            _mascotas.add(nombre)
        }
    }

    fun eliminarMascota(nombre: String) {
        _mascotas.remove(nombre)
    }

    fun obtenerMascotasDisponibles(): List<String> {
        val nombresMascotas = listOf("Killy", "Mango","Simba", "Niga")
        return nombresMascotas.filter { !mascotas.contains(it) }
    }
}