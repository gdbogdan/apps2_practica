package com.example.inpath.screens

import PosicionMascotaViewModel
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
    viewModel: MascotaViewModel = viewModel(),
    snackbarHostState: SnackbarHostState,
    redDisponible: Boolean,
    posicionViewModel: PosicionMascotaViewModel = viewModel()
){
    var mostrarMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val posicionMascota by posicionViewModel.posicion.collectAsState()

    // Lista de permisos necesarios
    val permisosUbicacion = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    // Launcher para solicitar permisos
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
        }
    }

    // Verifica permisos después de regresar de configuración
    fun verificarPermisos(): Boolean {
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
                    viewModel.eliminarMascota()
                    scope.launch {
                        val resultadoSnackbar = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.permisos_retirar_mensaje),
                            actionLabel = context.getString(R.string.configuracion)
                        )
                        if (resultadoSnackbar == SnackbarResult.ActionPerformed) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
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
                if (verificarPermisos()) {
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
                            if (viewModel.permisosConcedidos) {
                                viewModel.agregarMascota(nombre)
                                mostrarMenu = false
                            } else {
                                permisosLauncher.launch(permisosUbicacion)
                            }
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
