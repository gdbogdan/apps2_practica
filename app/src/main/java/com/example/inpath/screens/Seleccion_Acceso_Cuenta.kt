package com.example.inpath.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.inpath.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@SuppressLint("ContextCastToActivity")
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun Seleccion_Acceso_Cuenta(
    navHostController: NavHostController,
    auth: FirebaseAuth, // Recibimos FirebaseAuth, que es correcto
    snackbarHostState: SnackbarHostState // Recibimos SnackbarHostState
) {
    val context = LocalContext.current as Activity
    // Aseguramos que obtenemos el Application Context
    val application = context.applicationContext as Application

    val viewModelGoogle: GoogleSignInViewModel = viewModel(
        factory = GoogleSignInViewModelFactory(auth, application)
    )
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.pata),
            contentDescription = "Icono de una pata",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.inicio_sesion1),
            fontSize = 25.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón de inicio de sesión normal
        Button(
            onClick = {
                //throw RuntimeException("Test Crash") Solo para test de crashlytics
                navHostController.navigate("Inicio_sesion") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = stringResource(R.string.iniciar_sesion))
        }

        // --- Botón de Google modificado ---
        Boton_Personalizado(
            modifier = Modifier.padding(top = 8.dp),
            // Pasamos la lambda de acción al onClick de Boton_Personalizado
            onClick = {
                coroutineScope.launch {
                    Log.d("SeleccionAccesoCuenta", context.getString(R.string.intentando_iniciar_sesion_google))
                    val success = viewModelGoogle.signIn(context)
                    if (success) {
                        Log.d("SeleccionAccesoCuenta", context.getString(R.string.inicio_sesion_correcto_google))
                        navHostController.navigate("Seleccion_tipo_usuario")
                    } else {
                        Log.e("SeleccionAccesoCuenta", context.getString(R.string.error_inicio_sesion_google))
                    }
                }
            }
        )

        val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
        Text(
            text = stringResource(R.string.crear_cuenta),
            color = textColor,
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable(onClick = { navHostController.navigate("Crear_cuenta") })
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- Composable Boton_Personalizado (modificado para ser funcional) ---
@Composable
fun Boton_Personalizado(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val buttonShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(color = Color.White, shape = buttonShape)
            .clip(buttonShape)
            .border(2.dp, color = Color.Black, shape = buttonShape)
            // Aquí es donde se adjunta el clickable a la lambda `onClick`
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Image(
            painter = painterResource(id = R.drawable.google),
            contentDescription = "Logo de Google",
            modifier = Modifier
                .padding(start = 16.dp)
                .size(16.dp)
        )
        Text(
            text = stringResource(R.string.iniciar_sesion_google),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.Black
        )
    }
}