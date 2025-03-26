package com.example.inpath.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.inpath.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Propietario(
    navController: NavController,
    viewModel: PropietarioViewModel = viewModel(),
    snackbarHostState: SnackbarHostState
) {
    var mostrarDialogoAgregarMascota by remember { mutableStateOf(false) }
    var nombreMascota by remember { mutableStateOf("") }
    var sexoMascota by remember { mutableStateOf("") }
    var tipoMascota by remember { mutableStateOf("") }
    var expandedSexo by remember { mutableStateOf(false) }
    var expandedTipo by remember { mutableStateOf(false) }
    var localizacionActivada by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        viewModel.listaMascotas.forEach { mascota ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Nombre: ${mascota.nombre}")
                Text("Raza: ${mascota.tipo}")
                Text("Sexo: ${mascota.sexo}")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { localizacionActivada = true },
                        modifier = Modifier.background(if (localizacionActivada) Color.Green else Color.Red) // Corrección del background
                    ) {
                        Text("Localización")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            localizacionActivada = false
                            scope.launch {
                                val resultado = snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.geofence_en_desarrollo),
                                    actionLabel = "Aceptar" // Botón "Aceptar"
                                )
                                if (resultado == SnackbarResult.ActionPerformed) {
                                    // Se cierra el snackbar
                                }
                            }
                        },
                        modifier = Modifier.background(if (!localizacionActivada) Color.Green else Color.Red) // Corrección del background
                    ) {
                        Text("Geofence")
                    }
                }
            }
        }

        Button(onClick = { mostrarDialogoAgregarMascota = true }) {
            Text(stringResource(R.string.add_mascota))
        }
    }

    if (mostrarDialogoAgregarMascota) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAgregarMascota = false },
            title = { Text(stringResource(R.string.add_mascota)) },
            text = {
                Column {
                    // Nombre de la mascota
                    TextField(
                        value = nombreMascota,
                        onValueChange = { nombreMascota = it },
                        label = { Text(stringResource(R.string.nombre)) }
                    )

                    // Selector de Sexo
                    ExposedDropdownMenuBox(
                        expanded = expandedSexo,
                        onExpandedChange = { expandedSexo = it }
                    ) {
                        TextField(
                            readOnly = true,
                            value = sexoMascota,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.sexo)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSexo)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSexo,
                            onDismissRequest = { expandedSexo = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Macho") },
                                onClick = {
                                    sexoMascota = "Macho"
                                    expandedSexo = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hembra") },
                                onClick = {
                                    sexoMascota = "Hembra"
                                    expandedSexo = false
                                }
                            )
                        }
                    }

                    // Selector de Tipo
                    ExposedDropdownMenuBox(
                        expanded = expandedTipo,
                        onExpandedChange = { expandedTipo = it }
                    ) {
                        TextField(
                            readOnly = true,
                            value = tipoMascota,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.tipo)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTipo,
                            onDismissRequest = { expandedTipo = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Perro") },
                                onClick = {
                                    tipoMascota = "Perro"
                                    expandedTipo = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Gato") },
                                onClick = {
                                    tipoMascota = "Gato"
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nombreMascota.isNotBlank() && sexoMascota.isNotBlank() && tipoMascota.isNotBlank()) {
                        viewModel.agregarMascota(MascotaInfo(nombreMascota, sexoMascota, tipoMascota)) // Usando PropietarioViewModel
                        mostrarDialogoAgregarMascota = false
                        nombreMascota = ""
                        sexoMascota = ""
                        tipoMascota = ""
                    }
                }) {
                    Text(stringResource(R.string.add_mascota))
                }
            },
            dismissButton = {
                Button(onClick = { mostrarDialogoAgregarMascota = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}