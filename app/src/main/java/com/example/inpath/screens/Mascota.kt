package com.example.inpath.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inpath.R
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun Mascota(navController: NavController, viewModel: MascotaViewModel = viewModel(), snackbarHostState: SnackbarHostState) {
    var mostrarMenu by remember { mutableStateOf(false) }
    var mascotaSeleccionada by rememberSaveable { mutableStateOf(viewModel.mascotaSeleccionada) }

    //Permisos:
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var permisosSolicitados by rememberSaveable { mutableStateOf(false) }

    val permisosUbicacion = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    val permisosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        if (permisos.values.all { it }) {
            viewModel.permisosConcedidos(true)
        } else {
            viewModel.permisosConcedidos(false)
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

    LaunchedEffect(Unit) {
        if (viewModel.permisosConcedidos) {
            permisosLauncher.launch(permisosUbicacion)
            permisosSolicitados = true
        }
    }

    // Nueva función para verificar los permisos después de regresar de la configuración
    fun verificarPermisos() {
        if (permisosUbicacion.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }) {
            viewModel.permisosConcedidos(true)
        } else {
            viewModel.permisosConcedidos(false)
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
                }) {
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
                            if (!viewModel.permisosConcedidos) {
                                permisosLauncher.launch(permisosUbicacion)
                                permisosSolicitados = true
                            }
                            verificarPermisos() // Verifica los permisos después de seleccionar la mascota
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