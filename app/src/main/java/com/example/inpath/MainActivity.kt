package com.example.inpath

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            InPathTheme {
                val snackbar = remember{SnackbarHostState()}
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {TopAppBar()},
                    bottomBar = {BottomBar(navController)},
                    snackbarHost = { SnackbarHost(snackbar)}
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "Seleccion_tipo",
                        modifier = Modifier.padding(innerPadding)
                    ){
                        composable("Inicio"){
                            Inicio_sesion(navController)
                        }
                        composable("Seleccion_tipo"){
                            Seleccion_tipo(navController)
                        }
                        composable("Propietario"){
                            Propietario(navController)
                        }
                        composable("Mascota"){
                            Mascota(navController)
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
fun BottomBar(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Propietario") },
            label = { Text("Propietario") },
            selected = false,
            onClick = { navController.navigate("Propietario") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Info, contentDescription = "Mascota") },
            label = { Text("Mascota") },
            selected = false,
            onClick = { navController.navigate("Mascota") }
        )
    }
}
