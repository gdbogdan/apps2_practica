package com.example.inpath.screens

import Posicion
import PosicionMascotaViewModel
import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.inpath.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface LocationCallback {
    fun onLocationResult(locationResult: LocationResult)
}

data class LocationResult(val locations: List<Location>) {
    companion object {
        fun create(locations: List<Location>) = LocationResult(locations)
    }
}

data class Location(val provider: String) {
    var latitude: Double = 0.0
    var longitude: Double = 0.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Propietario(
    navController: NavController,
    viewModel: PropietarioViewModel = viewModel(),
    snackbarHostState: SnackbarHostState,
    redDisponible: Boolean,
    posicionViewModel: PosicionMascotaViewModel = viewModel()
) {
    var mostrarDialogoAgregarMascota by remember { mutableStateOf(false) }
    var nombreMascota by remember { mutableStateOf("") }
    var sexoMascota by remember { mutableStateOf("") }
    var tipoMascota by remember { mutableStateOf("") }
    var expandedSexo by remember { mutableStateOf(false) }
    var expandedTipo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var mascotaAEliminar by remember { mutableStateOf<MascotaInfo?>(null) }
    var modoEliminar by remember { mutableStateOf(false) }
    var localizacionActivada by remember { mutableStateOf(false) }
    var areaSeguraActivada by remember { mutableStateOf(false) }

    val posicionMascota by posicionViewModel.posicion.collectAsState()
    var ubicacionActual by remember { mutableStateOf(posicionMascota) }
    var mostrarMapa by remember { mutableStateOf(false) }

    val ubicacionCallback = remember {
        object : LocationCallback {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.lastOrNull()?.let { location ->
                    ubicacionActual = Posicion(location.latitude, location.longitude)
                }
            }
        }
    }

    fun requestLocationUpdates() {
        ubicacionCallback.onLocationResult(LocationResult.create(listOf(Location("").apply {
            latitude = posicionMascota.latitud
            longitude = posicionMascota.longitud
        })))
    }

    LaunchedEffect(Unit) {
        while (true) {
            requestLocationUpdates()
            delay(10000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState)
            .drawWithContent {
                drawContent()
                if (scrollState.maxValue > 0) {
                    val scrollPorcentaje =
                        scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                    val indicadorAltura = size.height * 0.1f
                    val indicadorY = size.height * scrollPorcentaje
                    drawRect(
                        color = Color.Gray,
                        topLeft = Offset(size.width - 8.dp.toPx(), indicadorY),
                        size = androidx.compose.ui.geometry.Size(8.dp.toPx(), indicadorAltura)
                    )
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        viewModel.listaMascotas.forEachIndexed { index, mascota ->
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
                    if (!modoEliminar) {
                        Button(
                            onClick = {
                                localizacionActivada = !localizacionActivada
                                areaSeguraActivada = false
                                viewModel.actualizarLocalizacion(index, localizacionActivada)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (localizacionActivada) Color.Green else Color.LightGray
                            )
                        ) {
                            Text(stringResource(R.string.localizacion))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                areaSeguraActivada = !areaSeguraActivada
                                localizacionActivada = false
                                viewModel.actualizarLocalizacion(index, !areaSeguraActivada)
                                scope.launch {
                                    val resultado = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.geofence_en_desarrollo),
                                        actionLabel = context.getString(R.string.aceptar)
                                    )
                                    if (resultado == SnackbarResult.ActionPerformed) {
                                        // Se cierra el snackbar
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (areaSeguraActivada) Color.Green else Color.LightGray
                            )
                        ) {
                            Text(stringResource(R.string.area_segura))
                        }
                    }
                    if (modoEliminar) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { mascotaAEliminar = mascota },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text(stringResource(R.string.eliminar))
                        }
                    }
                }
                if (!modoEliminar) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = {
                            mostrarMapa = true
                        }) {
                            Text(stringResource(R.string.mostrar_en_mapa))
                        }
                    }
                }
            }
        }

        Row {
            Button(onClick = { mostrarDialogoAgregarMascota = true }) {
                Text(stringResource(R.string.add_mascota))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { modoEliminar = !modoEliminar }) {
                Text(if (modoEliminar) stringResource(R.string.cancelar_eliminar) else stringResource(
                    R.string.eliminar_mascota
                ))
            }
        }
    }

    if (mostrarMapa) {
        MostrarMapa(ubicacionActual) {
            mostrarMapa = false
        }
    }

    if (mostrarDialogoAgregarMascota) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAgregarMascota = false },
            title = { Text(stringResource(R.string.add_mascota)) },
            text = {
                Column {
                    TextField(
                        value = nombreMascota,
                        onValueChange = { nombreMascota = it },
                        label = { Text(stringResource(R.string.nombre)) }
                    )
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
                                text = { Text(stringResource(R.string.macho)) },
                                onClick = {
                                    sexoMascota = "Macho"
                                    expandedSexo = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.hembra)) },
                                onClick = {
                                    sexoMascota = "Hembra"
                                    expandedSexo = false
                                }
                            )
                        }
                    }
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
                                text = { Text(stringResource(R.string.perro)) },
                                onClick = {
                                    tipoMascota = "Perro"
                                    expandedTipo = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.gato)) },
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
                        viewModel.agregarMascota(MascotaInfo(nombreMascota, sexoMascota, tipoMascota))
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
    if (mascotaAEliminar != null) {
        AlertDialog(
            onDismissRequest = { mascotaAEliminar = null },
            title = {
                Text(
                    text = stringResource(R.string.borrando_mascota, mascotaAEliminar?.nombre ?: ""),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = { Text(stringResource(R.string.aviso_no_deshacer)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.eliminarMascota(mascotaAEliminar!!)
                    mascotaAEliminar = null
                }) {
                    Text(stringResource(R.string.eliminar))
                }
            },
            dismissButton = {
                Button(onClick = { mascotaAEliminar = null }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}

@Composable
fun MostrarMapa(ubicacion: Posicion, onCerrar: () -> Unit) {
    val ubicacionLatLng = LatLng(ubicacion.latitud, ubicacion.longitud)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(ubicacionLatLng, 15f)
    }

    Column {
        Button(onClick = { onCerrar() }) {
            Text(stringResource(R.string.cerrar_mapa))
        }
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(state = MarkerState(position = ubicacionLatLng))
        }
    }
}