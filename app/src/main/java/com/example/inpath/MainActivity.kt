@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.example.inpath

import InternetViewModel
import PosicionMascotaViewModel
import PropietarioViewModel
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.inpath.screens.Crear_cuenta
import com.example.inpath.screens.Inicio_sesion
import com.example.inpath.screens.Mascota
import com.example.inpath.screens.Propietario
import com.example.inpath.screens.Seleccion_Acceso_Cuenta
import com.example.inpath.screens.Seleccion_tipo_usuario
import com.example.inpath.ui.theme.InPathTheme
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    @SuppressLint("ServiceCast")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        //Para realizar comprobaciones varias
        printSha1(this)

        super.onCreate(savedInstanceState)
        val apiKey = getString(R.string.google_maps_key)
        Log.d("MAPS_API_KEY", "Clave usada: $apiKey")

        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()
        setContent {
            val navHostController = rememberNavController()
            val internetViewModel: InternetViewModel = viewModel()
            val context = LocalContext.current
            val snackbarHostState = remember { SnackbarHostState() }
            val posicionMascotaViewModel: PosicionMascotaViewModel = viewModel()

            internetViewModel.comprobarDisponibilidadRed(context) //Compruebo si hay o no internet al inicial la app


            // Registrar NetworkCallback
            DisposableEffect(Unit) {
                val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
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

            val estadoRedState = internetViewModel.estadoRed.observeAsState()
            val redDisponible by internetViewModel.redDisponible.observeAsState(initial = true)
            val estadoRed = estadoRedState.value

            val currentBackStackEntry by navHostController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route

            LaunchedEffect(estadoRed) {
                estadoRed?.let {
                    if (it.isNotEmpty()) {
                        snackbarHostState.showSnackbar(it)
                    }
                }
            }

            InPathTheme {
                var esPropietario by rememberSaveable { mutableStateOf(false) }

                val hideTopBottomBarOnRoutes = listOf("Seleccion_Acceso_Cuenta", "Inicio_sesion", "Crear_cuenta")
                val shouldShowTopBottomBar = currentRoute !in hideTopBottomBarOnRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { if (shouldShowTopBottomBar) TopAppBar() },
                    bottomBar = {
                        if (shouldShowTopBottomBar) {
                            BottomBar(
                                navHostController,
                                esPropietario,
                                cambioPropietario = { esPropietario = it },
                                redDisponible = redDisponible
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    if (redDisponible == true) {
                        NavHost(
                            navController = navHostController,
                            startDestination = "Seleccion_Acceso_Cuenta",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("Seleccion_Acceso_Cuenta") {
                                Seleccion_Acceso_Cuenta(navHostController, auth, snackbarHostState)
                            }
                            composable("Inicio_sesion") {
                                Inicio_sesion(navHostController, auth, snackbarHostState = snackbarHostState)
                            }
                            composable ("Crear_cuenta"){
                                Crear_cuenta(navHostController, auth, snackbarHostState = snackbarHostState)
                            }
                            composable("Seleccion_tipo_usuario") {
                                val redDisponibleState by internetViewModel.redDisponible.observeAsState(initial = false)
                                Seleccion_tipo_usuario(
                                    navHostController,
                                    redDisponible = redDisponibleState
                                )
                            }
                            composable("Propietario") {
                                val redDisponibleState by internetViewModel.redDisponible.observeAsState(initial = false)
                                Propietario(
                                    navController = navHostController,
                                    viewModel = viewModel<PropietarioViewModel>(),
                                    snackbarHostState = snackbarHostState,
                                    redDisponible = redDisponibleState,
                                    posicionViewModel = posicionMascotaViewModel
                                )
                            }
                            composable("Mascota") {
                                val redDisponibleState by internetViewModel.redDisponible.observeAsState(initial = false)
                                Mascota(
                                    navHostController,
                                    snackbarHostState = snackbarHostState,
                                    redDisponible = redDisponibleState,
                                    posicionViewModel = posicionMascotaViewModel
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.sinConexion),
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

@RequiresApi(Build.VERSION_CODES.P)
fun printSha1(context: Context) {
    val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    val cert = info.signingInfo?.apkContentsSigners[0]?.toByteArray()
    val md = MessageDigest.getInstance("SHA1")
    val sha1 = md.digest(cert)
    val hex = sha1.joinToString(":") { "%02X".format(it) }
    Log.d("SHA1_DEBUG", "App está firmada con: $hex")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(){
    CenterAlignedTopAppBar(
        title={
            Text(text = stringResource(R.string.top_bar),
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
                    contentDescription = stringResource(R.string.icono_del_propietario)
                )
            },
            label = { Text(stringResource(R.string.propietario)) },
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
                    contentDescription = stringResource(R.string.icono_de_la_huella_de_la_mascota)
                )
            },
            label = { Text(stringResource(R.string.mascota)) },
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