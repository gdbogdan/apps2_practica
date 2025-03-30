package com.example.inpath.screens

import AreaSegura
import MascotaInfo
import Posicion
import PosicionMascotaViewModel
import PropietarioViewModel
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch

interface LocationCallback {
    fun onLocationResult(locationResult: LocationResult)
}

data class LocationResult(val locations: List<Location>) {
    companion object {
        fun create(locations: List<Location>) = LocationResult(locations)
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
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
    var mostrarDialogoAreaSegura by remember { mutableStateOf(false) }
    var areaSeguraSeleccionada by remember { mutableStateOf<AreaSegura?>(null) }

    var mostrandoCrearAreaDialog by remember { mutableStateOf(false) }
    var mostrandoEliminarAreaDialog by remember { mutableStateOf(false) }

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
        viewModel.listaMascotas.value.forEachIndexed { index, mascota ->
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
                                mostrarDialogoAreaSegura = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (areaSeguraSeleccionada != null) Color.Green else Color.LightGray
                            )
                        ) {
                            Text(if (areaSeguraSeleccionada != null) areaSeguraSeleccionada?.nombre ?: stringResource(R.string.area_segura) else stringResource(R.string.area_segura))
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

    if (mostrarDialogoAreaSegura) {
        SeleccionarAreaSeguraDialog(
            onDismiss = { mostrarDialogoAreaSegura = false },
            onAreaSeleccionada = { area ->
                areaSeguraSeleccionada = area
                areaSeguraActivada = true
                localizacionActivada = false
                //viewModel.actualizarLocalizacion(index, !areaSeguraActivada)
                scope.launch {
                    val resultado = snackbarHostState.showSnackbar(
                        message = "Área seleccionada: ${area.nombre}"
                    )
                    if (resultado == SnackbarResult.ActionPerformed) {
                        // Se cierra el snackbar
                    }
                }
            },
            snackbarHostState = snackbarHostState, // Pasa snackbarHostState
            scope = scope // Pasa scope
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionarAreaSeguraDialog(
    onDismiss: () -> Unit,
    onAreaSeleccionada: (AreaSegura) -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val viewModel: PropietarioViewModel = viewModel()
    val areas by viewModel.areasSeguras.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var areaSeleccionada by remember { mutableStateOf<AreaSegura?>(null) }
    var mostrarCrearAreaDialog by remember { mutableStateOf(false) }
    var areaAEliminar by remember { mutableStateOf<AreaSegura?>(null) }
    var modoEliminarArea by remember { mutableStateOf(false) } // Nuevo estado

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Área Segura") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        readOnly = true,
                        value = areaSeleccionada?.nombre ?: "",
                        onValueChange = {},
                        label = { Text("Área") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        areas.forEach { area ->
                            DropdownMenuItem(
                                text = { Text(area.nombre) },
                                onClick = {
                                    areaSeleccionada = area
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { mostrarCrearAreaDialog = true }) {
                        Text("Crear Área")
                    }
                    Button(onClick = { modoEliminarArea = !modoEliminarArea }) {
                        Text(if (modoEliminarArea) "Cancelar Eliminar" else "Eliminar Área")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (areaSeleccionada != null) {
                    Button(onClick = {
                        onAreaSeleccionada(areaSeleccionada!!)
                        scope.launch {
                            snackbarHostState.showSnackbar("Área seleccionada: ${areaSeleccionada!!.nombre}")
                        }
                    }) {
                        Text("Seleccionar")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )

    if (mostrarCrearAreaDialog) {
        CrearAreaSeguraDialog(
            onDismiss = { mostrarCrearAreaDialog = false },
            onAreaCreada = { nuevaArea ->
                viewModel.agregarAreaSegura(nuevaArea)
                scope.launch {
                    snackbarHostState.showSnackbar("Área ${nuevaArea.nombre} creada correctamente")
                }
            },
            scope = scope,
            snackbarHostState = snackbarHostState
        )
    }

    if (modoEliminarArea && areaSeleccionada != null) {
        AlertDialog(
            onDismissRequest = { modoEliminarArea = false },
            title = {
                Text(
                    text = "Eliminar área ${areaSeleccionada?.nombre ?: ""}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = { Text("¿Estás seguro de que quieres eliminar esta área? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.eliminarAreaSegura(areaSeleccionada!!)
                    scope.launch {
                        snackbarHostState.showSnackbar("Área ${areaSeleccionada!!.nombre} eliminada correctamente")
                    }
                    modoEliminarArea = false
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                Button(onClick = { modoEliminarArea = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CrearAreaSeguraDialog(
    onDismiss: () -> Unit,
    onAreaCreada: (AreaSegura) -> Unit,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    var nombreArea by remember { mutableStateOf("") }
    var latitudArea by remember { mutableStateOf("") }
    var longitudArea by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Área Segura") },
        text = {
            Column {
                TextField(
                    value = nombreArea,
                    onValueChange = { nombreArea = it },
                    label = { Text("Nombre") }
                )
                TextField(
                    value = latitudArea,
                    onValueChange = { latitudArea = it },
                    label = { Text("Latitud") }
                )
                TextField(
                    value = longitudArea,
                    onValueChange = { longitudArea = it },
                    label = { Text("Longitud") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val nuevaArea = AreaSegura(
                    nombre = nombreArea,
                    latitud = latitudArea.toDouble(),
                    longitud = longitudArea.toDouble()
                )
                onAreaCreada(nuevaArea)
                onDismiss() // Cierra el diálogo después de crear el área
            }) {
                Text("Crear Área")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}