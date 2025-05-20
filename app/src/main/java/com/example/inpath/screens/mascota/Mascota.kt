package com.example.inpath.screens.mascota

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.inpath.R
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun Mascota(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    redDisponible: Boolean // Puedes mantenerlo si lo usas en otras partes
){
    val context = LocalContext.current
    val viewModel: MascotaViewModel = viewModel(
        factory = MascotaViewModel.MascotaViewModelFactory(context.applicationContext)
    )
    var mostrarMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val posicionMascota by viewModel.posicionMascota.collectAsState()

    // Lista de permisos necesarios (asegúrate de incluir BACKGROUND_LOCATION si es para apps que se ejecutan en background)
    val permisosUbicacion = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION // Para Android 10+ si quieres rastreo en background
    )

    val permisosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val todosConcedidos = permisos.values.all { it }
        viewModel.verificarPermisosYActualizar(todosConcedidos)

        if (!todosConcedidos) {
            scope.launch {
                val resultadoSnackbar = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.permisos_ubicacion_denegados),
                    actionLabel = context.getString(R.string.configuracion)
                )
                if (resultadoSnackbar == SnackbarResult.ActionPerformed) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        } else {
            // Si los permisos se acaban de conceder, y hay una mascota seleccionada, iniciar el rastreo
            viewModel.mascotaSeleccionada?.let {
                viewModel.startLocationUpdatesParaMascota()
            }
        }
    }

    // REALTIME DATABASE - CARGAR MASCOTAS y OBSERVAR POSICIÓN
    LaunchedEffect(Unit) {
        viewModel.cargarMascotasDesdeFirebase()
    }

    LaunchedEffect(viewModel.mascotaSeleccionada) {
        viewModel.mascotaSeleccionada?.let {
            viewModel.observarPosicionDesdeFirebase()
        }
    }

    // Verifica permisos al inicio y después de regresar de configuración
    fun verificarPermisosActuales(): Boolean {
        return permisosUbicacion.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }.also { permisosOk ->
            viewModel.verificarPermisosYActualizar(permisosOk)
        }
    }

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
                Button(onClick = {
                    viewModel.eliminarMascota() // Elimina la mascota y detiene el rastreo asociado
                    scope.launch {
                        // El Snackbar para informar sobre la deselección/eliminación
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.mascota_eliminada_deseleccionada), // Nuevo string o adaptar
                            actionLabel = context.getString(R.string.aceptar)
                        )
                    }
                }) {
                    Text(stringResource(R.string.eliminar))
                }
            }
            Text(
                text = stringResource(R.string.posicion_mascota),
                fontSize = 20.sp
            )
            Text(
                text = stringResource(R.string.latitud, posicionMascota.latitud.toString()),
                fontSize = 16.sp
            )
            Text(
                text = stringResource(R.string.longitud, posicionMascota.longitud.toString()),
                fontSize = 16.sp
            )
        } else {
            Button(onClick = {
                if (verificarPermisosActuales()) {
                    mostrarMenu = true
                } else {
                    permisosLauncher.launch(permisosUbicacion)
                }
            }) {
                Text(stringResource(R.string.add_mascota))
            }
        }
    }

    if (mostrarMenu) {
        val mascotas by viewModel.mascotasDisponibles.collectAsState()
        AlertDialog(
            onDismissRequest = { mostrarMenu = false },
            title = { Text(stringResource(R.string.seleccionar_mascota)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (mascotas.isEmpty()) {
                        Text(stringResource(R.string.no_mascotas_disponibles))
                    } else {
                        mascotas.forEach { nombre ->
                            Button(onClick = {
                                if (viewModel.permisosConcedidos) {
                                    viewModel.seleccionarMascota(nombre)
                                    mostrarMenu = false
                                } else {
                                    permisosLauncher.launch(permisosUbicacion)
                                }
                            }) {
                                Text(nombre)
                            }
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