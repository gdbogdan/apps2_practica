package com.example.inpath.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inpath.R

@Composable
fun Mascota(navController: NavController, viewModel: MascotaViewModel = viewModel()) {
    var mostrarMenu by remember { mutableStateOf(false) }
    var mascotaSeleccionada by rememberSaveable { mutableStateOf(viewModel.mascotaSeleccionada) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (viewModel.mascotaSeleccionada != null) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(viewModel.mascotaSeleccionada!!)
                Button(onClick = { viewModel.eliminarMascota() }) {
                    Text(stringResource(R.string.eliminar))
                }
            }
        } else {
            Button(onClick = { mostrarMenu = true }) {
                Text(stringResource(R.string.add_mascota))
            }
        }
    }

    if (mostrarMenu) {
        AlertDialog(
            onDismissRequest = { mostrarMenu = false },
            title = { Text(stringResource(R.string.seleccionar_mascota)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    viewModel.obtenerMascotasDisponibles().forEach { nombre ->
                        Button(onClick = {
                            mascotaSeleccionada = nombre
                            viewModel.agregarMascota(nombre)
                            mostrarMenu = false
                        }) {
                            Text(nombre)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { mostrarMenu = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}