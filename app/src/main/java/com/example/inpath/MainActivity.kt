package com.example.inpath

import InternetViewModel
import PosicionMascotaViewModel
import PropietarioViewModel
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inpath.R.string
import com.example.inpath.screens.Inicio_sesion
import com.example.inpath.screens.Mascota
import com.example.inpath.screens.Propietario
import com.example.inpath.screens.Seleccion_tipo
import com.example.inpath.ui.theme.InPathTheme


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val internetViewModel: InternetViewModel = viewModel()
            val context = LocalContext.current
            val snackbarHostState = remember { SnackbarHostState() }
            val posicionMascotaViewModel: PosicionMascotaViewModel = viewModel()

            internetViewModel.comprobarDisponibilidadRed(context) //Compruebo si hay o no internet al inicial la app

            // Registrar NetworkCallback
            DisposableEffect(Unit) {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkRequest = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                val networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        internetViewModel.comprobarDisponibilidadRed(context)
                    }

                    override fun onLost(network: Network) {
                        internetViewModel.comprobarDisponibilidadRed(context)
                    }

                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                        internetViewModel.comprobarDisponibilidadRed(context)
                    }
                }
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
                onDispose {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
            }

            val estadoRed by internetViewModel.estadoRed.observeAsState()
            val redDisponible by internetViewModel.redDisponible.observeAsState(initial = true) // Asumo que hay internet

            LaunchedEffect(estadoRed) {
                estadoRed?.let {
                    if (it.isNotEmpty()) {
                        snackbarHostState.showSnackbar(it)
                    }
                }
            }

            InPathTheme {
                var esPropietario by rememberSaveable { mutableStateOf(false) } //true -> propietario, false -> mascota (default)
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopAppBar() },
                    bottomBar = { BottomBar(navController, esPropietario, cambioPropietario = { esPropietario = it }, redDisponible = redDisponible) }, //Paso redDisponible
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    if (redDisponible) { // Solo muestra la navegación si hay conexión
                        NavHost(
                            navController = navController,
                            startDestination = "Seleccion_tipo",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("Inicio") {
                                Inicio_sesion(navController)
                            }
                            composable("Seleccion_tipo") {
                                val redDisponibleState by internetViewModel.redDisponible.observeAsState(initial = false)
                                Seleccion_tipo(navController, redDisponible = redDisponibleState)
                            }
                            composable("Propietario") {
                                val redDisponibleState by internetViewModel.redDisponible.observeAsState(initial = false)
                                Propietario(navController, viewModel = viewModel<PropietarioViewModel>(), snackbarHostState = snackbarHostState, redDisponible = redDisponibleState, posicionViewModel = posicionMascotaViewModel)
                            }
                            composable("Mascota") {
                                val redDisponibleState by internetViewModel.redDisponible.observeAsState(initial = false)
                                Mascota(navController, snackbarHostState = snackbarHostState, redDisponible = redDisponibleState, posicionViewModel = posicionMascotaViewModel)
                            }
                        }
                    } else {
                        // Muestra un mensaje o una pantalla de error si no hay conexión
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(string.sinConexion),
                                modifier = Modifier.padding(innerPadding),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(){
    CenterAlignedTopAppBar(
        title={
            Text(text = stringResource(string.top_bar),
                color = androidx.compose.ui.graphics.Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = androidx.compose.ui.graphics.Color.White
        )
    )
}

@Composable
fun BottomBar(navController: NavController, esPropietario: Boolean, cambioPropietario: (Boolean) -> Unit, redDisponible: Boolean) {
    NavigationBar {
        NavigationBarItem(
            icon = {
                Icon(
                    painterResource(id = R.drawable.usuario),
                    contentDescription = stringResource(string.icono_del_propietario)
                )
            },
            label = { Text(stringResource(string.propietario)) },
            selected = esPropietario,
            onClick = {
                if (redDisponible) {
                    cambioPropietario(true)
                    navController.navigate("Propietario") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painterResource(id = R.drawable.pata),
                    contentDescription = stringResource(string.icono_de_la_huella_de_la_mascota)
                )
            },
            label = { Text(stringResource(string.mascota)) },
            selected = !esPropietario,
            onClick = {
                if (redDisponible) {
                    cambioPropietario(false)
                    navController.navigate("Mascota") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            }
        )
    }
}