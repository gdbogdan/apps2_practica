package com.example.inpath.screens.inicio_sesion

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.inpath.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun Inicio_sesion(
    navHostController: NavHostController,
    auth: FirebaseAuth,
    snackbarHostState: SnackbarHostState
){
    var email:String by remember { mutableStateOf("") }
    var password:String by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.iniciar_sesion_g),
            fontSize = 36.sp
        )
        Spacer(Modifier.height(36.dp))
        Text(
            text = stringResource(R.string.usuario),
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth() )
        Spacer(Modifier.height(48.dp))
        Text(
            text = stringResource(R.string.contraseña),
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(48.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ){
            Button(
                onClick = {
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.i("Inicio sesion", "Inicio OK")
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.inicio_sesion_exito),
                                    duration = SnackbarDuration.Long
                                )
                            }
                            navHostController.navigate("Seleccion_tipo_usuario")
                        } else {
                            Log.i("Inicio sesion", "Inicio KO")
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.inicio_sesion_error),
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.iniciar_sesion)
                )
            }
        }

        val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
        Text(
            text = stringResource(R.string.crear_cuenta2),
            color = textColor,
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable(onClick = { navHostController.navigate("Crear_cuenta") })
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.otro_metodo),
            color = textColor,
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable(onClick = { navHostController.navigate("Seleccion_Acceso_Cuenta") })
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}