package com.example.inpath.screens.propietario

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.inpath.R
import com.example.inpath.screens.mascota.MascotaFirebase
import com.example.inpath.screens.Posicion
import com.example.inpath.screens.propietario.PropietarioViewModel
import com.example.inpath.screens.geofence.AreaSegura
import com.example.inpath.screens.geofence.GeoFenceManager
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Propietario(
    navController: NavController,
    viewModel: PropietarioViewModel = viewModel(),
    snackbarHostState: SnackbarHostState,
    redDisponible: Boolean
) {
    val mascotas by viewModel.mascotasFirebase.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var mostrarDialogoAgregarMascota by remember { mutableStateOf(false) }
    var mostrarDialogoAreaSegura by remember { mutableStateOf(false) }
    var mascotaAEliminar by remember { mutableStateOf<MascotaFirebase?>(null) }
    var areaEditando by remember { mutableStateOf<AreaSegura?>(null) }

    // --- NUEVA VARIABLE DE ESTADO PARA MOSTRAR EL MAPA ---
    var mostrarMapaDeMascota by remember { mutableStateOf(false) }
    // --- NUEVO: Recolectar la posición actual de la mascota para el mapa ---
    val posicionMascotaParaMapa by viewModel.posicionMascotaMostrada.collectAsState()


    //Permisos ubicación:
    val permisosUbicacion = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    val permisosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        val todosConcedidos = resultado.values.all { it }
        if (!todosConcedidos) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.permisos_geofence_denegados))
            }
        }
    }

    var modoEliminar by remember { mutableStateOf(false) }

    var areaSeguraSeleccionada by remember { mutableStateOf<AreaSegura?>(null) }
    var mascotaParaAreaSegura by remember { mutableStateOf<MascotaFirebase?>(null) }

    val estadoRastreo = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(mascotas) {
        mascotas.forEach { mascota ->
            estadoRastreo[mascota.nombre] = mascota.tipoRastreo ?: ""
        }
    }


    val geoFenceManager = remember { GeoFenceManager(context) }
    LaunchedEffect(geoFenceManager) {
        viewModel.setGeoFenceManager(geoFenceManager)
    }

    LaunchedEffect(Unit) {
        viewModel.observarMascotasDesdeFirebase()
        viewModel.observarAreasSegurasDesdeFirebase()
    }

    val geofenceSnackbarEvent by viewModel.geofenceSnackbarEvents.collectAsState()
    LaunchedEffect(geofenceSnackbarEvent) {
        geofenceSnackbarEvent?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.consumeGeofenceSnackbarEvent()
            }
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
                        size = Size(8.dp.toPx(), indicadorAltura)
                    )
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- LISTADO DE MASCOTAS ---
        if (mascotas.isEmpty()) {
            Text(stringResource(R.string.no_mascotas_disponibles), modifier = Modifier.padding(16.dp))
        } else {
            mascotas.forEach { mascota ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Nombre: ${mascota.nombre}")
                    Text("Raza: ${mascota.tipo}")
                    Text("Sexo: ${mascota.sexo}")

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        if (!modoEliminar) {
                            val rastreoActualParaBoton = estadoRastreo[mascota.nombre] ?: mascota.tipoRastreo ?: ""

                            // Botón de Localización (ahora solo indica al dispositivo mascota que "simule" si no hay GPS real)
                            Button(
                                onClick = {
                                    val nuevoTipo = if (rastreoActualParaBoton == "Localizacion") "" else "Localizacion"
                                    estadoRastreo[mascota.nombre] = nuevoTipo
                                    viewModel.actualizarTipoRastreo(mascota.nombre, nuevoTipo)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (rastreoActualParaBoton == "Localizacion") Color.Green else Color.LightGray
                                )
                            ) {
                                Text(stringResource(R.string.localizacion))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Botón de Área Segura
                            Button(
                                onClick = {
                                    val nuevoTipo = if (rastreoActualParaBoton == "AreaSegura") "" else "AreaSegura"
                                    estadoRastreo[mascota.nombre] = nuevoTipo

                                    if (nuevoTipo == "AreaSegura") {
                                        mascotaParaAreaSegura = mascota
                                        mostrarDialogoAreaSegura = true
                                    } else {
                                        viewModel.actualizarTipoRastreo(mascota.nombre, "", null)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (rastreoActualParaBoton == "AreaSegura") Color.Green else Color.LightGray
                                )
                            ) {
                                val textoBotonArea = if (mascota.tipoRastreo == "AreaSegura" && !mascota.nombreArea.isNullOrBlank()) {
                                    mascota.nombreArea
                                } else {
                                    stringResource(R.string.area_segura)
                                }
                                Text(textoBotonArea)
                            }
                        } else { // MODO ELIMINAR
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
                                viewModel.observarPosicionParaMapa(mascota.nombre)
                                mostrarMapaDeMascota = true // Mostrar el mapa
                            }) {
                                Text(stringResource(R.string.mostrar_en_mapa))
                            }
                        }
                    }
                }
            }
        }

        // --- BOTONES INFERIORES ---
        Row {
            Button(onClick = { mostrarDialogoAgregarMascota = true }) {
                Text(stringResource(R.string.add_mascota))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { modoEliminar = !modoEliminar }) {
                Text(
                    if (modoEliminar)
                        stringResource(R.string.cancelar_eliminar)
                    else
                        stringResource(R.string.eliminar_mascota)
                )
            }
        }
    }

    // --- DIÁLOGOS ---

    if (mostrarDialogoAreaSegura) {
        SeleccionarAreaSeguraDialog(
            onDismiss = { mostrarDialogoAreaSegura = false },
            onAreaSeleccionada = { area ->
                areaSeguraSeleccionada = area
                mascotaParaAreaSegura?.let { mascota ->
                    estadoRastreo[mascota.nombre] = "AreaSegura"
                    if (permisosUbicacion.all { permiso ->
                            ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED
                        }) {
                        // Aquí se llama a actualizarTipoRastreo en el ViewModel,
                        // que se encargará de añadir la geocerca.
                        viewModel.actualizarTipoRastreo(mascota.nombre, "AreaSegura", area)
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.geofence_activa_para, mascota.nombre, area.nombre))
                        }
                    } else {
                        permisosLauncher.launch(permisosUbicacion)
                    }
                }
                mostrarDialogoAreaSegura = false
            },
            onEditarArea = { area ->
                areaEditando = area
                mostrarDialogoAreaSegura = false
            },
            snackbarHostState = snackbarHostState,
            scope = scope,
            viewModel = viewModel
        )
    }

    if (areaEditando != null) {
        EditarAreaSeguraDialog(
            areaOriginal = areaEditando!!,
            onDismiss = { areaEditando = null },
            viewModel = viewModel,
            scope = scope,
            snackbarHostState = snackbarHostState
        )
    }

    if (mostrarDialogoAgregarMascota) {
        AgregarMascotaDialog(
            onDismiss = { mostrarDialogoAgregarMascota = false },
            onMascotaAgregada = { nombre, sexo, tipo ->
                val nuevaMascota = MascotaFirebase(
                    nombre = nombre,
                    tipo = tipo,
                    sexo = sexo,
                    latitud = 0.0,
                    longitud = 0.0,
                    seleccionada = false,
                    tipoRastreo = "" // Inicialmente sin tipo de rastreo
                )
                viewModel.subirMascotaAFirebase(
                    nuevaMascota,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.mascota_agregada_exito))
                        }
                    },
                    onFailure = {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.error_agregar_mascota, it.message ?: "desconocido"))
                        }
                    }
                )
                mostrarDialogoAgregarMascota = false
            },
            snackbarHostState = snackbarHostState,
            scope = scope,
            redDisponible = redDisponible
        )
    }

    // --- IMPLEMENTACIÓN DEL MAPA ---
    // Solo mostramos el mapa si 'mostrarMapaDeMascota' es true y tenemos una posición válida
    if (mostrarMapaDeMascota && posicionMascotaParaMapa.latitud != 0.0 && posicionMascotaParaMapa.longitud != 0.0) {
        MostrarMapa(posicionMascotaParaMapa) {
            mostrarMapaDeMascota = false
            // Opcional: Detener la observación de la posición cuando se cierra el mapa
            viewModel.detenerObservacionPosicionMapa()
        }
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
                    viewModel.eliminarMascotaDeFirebase(
                        nombreMascota = mascotaAEliminar!!.nombre,
                        onSuccess = {
                            mascotaAEliminar = null
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.mascota_eliminada_exito))
                            }
                        },
                        onFailure = {
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.error_eliminar_mascota, it.message ?: "desconocido"))
                            }
                        }
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionarAreaSeguraDialog(
    onDismiss: () -> Unit,
    onAreaSeleccionada: (AreaSegura) -> Unit,
    onEditarArea: (AreaSegura) -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    viewModel: PropietarioViewModel
) {
    val areas by viewModel.areasSeguras.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var areaSeleccionadaEnDialog by remember { mutableStateOf<AreaSegura?>(null) }
    var mostrarCrearAreaDialog by remember { mutableStateOf(false) }
    var areaAEliminar by remember { mutableStateOf<AreaSegura?>(null) }
    var mostrarDialogoConfirmacionEliminarArea by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.seleccionar_gestionar_area_segura)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (areas.isEmpty()) {
                    Text(stringResource(R.string.no_areas_seguras_disponibles), modifier = Modifier.padding(vertical = 8.dp))
                }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        readOnly = true,
                        value = areaSeleccionadaEnDialog?.nombre ?: stringResource(R.string.selecciona_un_area),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.area_existente)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        areas.forEach { area ->
                            DropdownMenuItem(
                                text = { Text(area.nombre) },
                                onClick = {
                                    areaSeleccionadaEnDialog = area
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = { mostrarCrearAreaDialog = true }) {
                        Text(stringResource(R.string.crear_nueva_area))
                    }
                    Button(
                        onClick = {
                            areaSeleccionadaEnDialog?.let { onEditarArea(it) }
                        },
                        enabled = areaSeleccionadaEnDialog != null
                    ) {
                        Text(stringResource(R.string.editar_area))
                    }
                    Button(
                        onClick = {
                            if (areaSeleccionadaEnDialog != null) {
                                areaAEliminar = areaSeleccionadaEnDialog
                                mostrarDialogoConfirmacionEliminarArea = true
                            } else {
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.selecciona_area_eliminar)) }
                            }
                        },
                        enabled = areaSeleccionadaEnDialog != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text(stringResource(R.string.eliminar_area))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (areaSeleccionadaEnDialog != null) {
                    Button(
                        onClick = {
                            onAreaSeleccionada(areaSeleccionadaEnDialog!!)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.asociar_area_seleccionada))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cerrar))
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false)
    )

    if (mostrarCrearAreaDialog) {
        CrearAreaSeguraDialog(
            onDismiss = { mostrarCrearAreaDialog = false },
            viewModel = viewModel,
            scope = scope,
            snackbarHostState = snackbarHostState
        )
    }

    if (mostrarDialogoConfirmacionEliminarArea) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoConfirmacionEliminarArea = false; areaAEliminar = null },
            title = { Text(stringResource(R.string.eliminar_area_segura_nombre, areaAEliminar?.nombre ?: "")) },
            text = { Text(stringResource(R.string.aviso_eliminar_area_segura)) },
            confirmButton = {
                Button(onClick = {
                    areaAEliminar?.let { area ->
                        viewModel.eliminarAreaSegura(area)
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.area_eliminada_exito, area.nombre)) }
                        areaAEliminar = null
                        areaSeleccionadaEnDialog = null
                        mostrarDialogoConfirmacionEliminarArea = false
                    }
                }) {
                    Text(stringResource(R.string.confirmar_eliminacion), color = Color.Red)
                }
            },
            dismissButton = {
                Button(onClick = { mostrarDialogoConfirmacionEliminarArea = false; areaAEliminar = null }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearAreaSeguraDialog(
    onDismiss: () -> Unit,
    viewModel: PropietarioViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    var nombre by remember { mutableStateOf("") }
    var latitud by remember { mutableStateOf("") }
    var longitud by remember { mutableStateOf("") }
    var radio by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.crear_nueva_area_segura)) },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.nombre_del_area)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = latitud,
                    onValueChange = { latitud = it },
                    label = { Text(stringResource(R.string.latitud)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = longitud,
                    onValueChange = { longitud = it },
                    label = { Text(stringResource(R.string.longitud)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = radio,
                    onValueChange = { radio = it },
                    label = { Text(stringResource(R.string.radio_metros)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                try {
                    val nuevaArea = AreaSegura(
                        nombre = nombre,
                        latitud = latitud.toDouble(),
                        longitud = longitud.toDouble(),
                        radio = radio.toFloat()
                    )
                    viewModel.agregarAreaSegura(nuevaArea)
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.area_creada_exito, nombre)) }
                    onDismiss()
                } catch (e: Exception) {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_datos_invalidos, e.message ?: "desconocido")) }
                }
            }) {
                Text(stringResource(R.string.crear))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancelar))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarAreaSeguraDialog(
    areaOriginal: AreaSegura,
    onDismiss: () -> Unit,
    viewModel: PropietarioViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    var nombre by remember { mutableStateOf(areaOriginal.nombre) }
    var latitud by remember { mutableStateOf(areaOriginal.latitud.toString()) }
    var longitud by remember { mutableStateOf(areaOriginal.longitud.toString()) }
    var radio by remember { mutableStateOf(areaOriginal.radio.toString()) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editar_area_segura_nombre, areaOriginal.nombre)) },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.nombre_del_area)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = latitud,
                    onValueChange = { latitud = it },
                    label = { Text(stringResource(R.string.latitud)) },
                    modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = longitud,
                    onValueChange = { longitud = it },
                    label = { Text(stringResource(R.string.longitud)) },
                    modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = radio,
                    onValueChange = { radio = it },
                    label = { Text(stringResource(R.string.radio_metros)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                try {
                    val areaActualizada = AreaSegura(
                        nombre = nombre,
                        latitud = latitud.toDouble(),
                        longitud = longitud.toDouble(),
                        radio = radio.toFloat()
                    )
                    viewModel.actualizarAreaSegura(areaOriginal, areaActualizada)
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.area_actualizada_exito, nombre)) }
                    onDismiss()
                } catch (e: Exception) {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_datos_invalidos, e.message ?: "desconocido")) }
                }
            }) {
                Text(stringResource(R.string.actualizar))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancelar))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarMascotaDialog(
    onDismiss: () -> Unit,
    onMascotaAgregada: (String, String, String) -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    redDisponible: Boolean
) {
    var nombre by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var expandedTipo by remember { mutableStateOf(false) }
    var expandedSexo by remember { mutableStateOf(false) }

    val tiposMascota = listOf("Perro", "Gato", "Pájaro", "Otros")
    val sexosMascota = listOf("Macho", "Hembra")
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.agregar_nueva_mascota)) },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.nombre)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.tipo_de_mascota)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        tiposMascota.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    tipo = selectionOption
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedSexo,
                    onExpandedChange = { expandedSexo = !expandedSexo }
                ) {
                    OutlinedTextField(
                        value = sexo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.sexo)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSexo) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSexo,
                        onDismissRequest = { expandedSexo = false }
                    ) {
                        sexosMascota.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    sexo = selectionOption
                                    expandedSexo = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank() && tipo.isNotBlank() && sexo.isNotBlank()) {
                        if (!redDisponible) {
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.no_internet_add_mascota))
                            }
                        } else {
                            onMascotaAgregada(nombre, sexo, tipo)
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.rellena_todos_los_campos))
                        }
                    }
                }
            ) {
                Text(text = stringResource(R.string.agregar))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancelar))
            }
        }
    )
}

@Composable
fun MostrarMapa(ubicacion: Posicion, onCerrar: () -> Unit) {
    val ubicacionLatLng = LatLng(ubicacion.latitud, ubicacion.longitud)
    // Usamos rememberCameraPositionState para controlar la posición de la cámara
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(ubicacionLatLng, 15f)
    }

    Column(Modifier.fillMaxSize()) {
        Button(onClick = { onCerrar() }, modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.cerrar_mapa))
        }
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(state = MarkerState(position = ubicacionLatLng), title = "Ubicación de la mascota")
        }
    }
}