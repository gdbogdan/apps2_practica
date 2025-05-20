package com.example.inpath.screens

data class MascotaFirebase(
    val nombre: String = "",
    val sexo: String = "",
    val tipo: String = "",
    val tipoRastreo: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val seleccionada: Boolean = false
)