package com.example.inpath.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.inpath.R
import androidx.compose.foundation.isSystemInDarkTheme


@Composable
fun Seleccion_Acceso_Cuenta(navHostController: NavHostController) {
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

        //Usuario y contraseña
        Button(
            onClick = { navHostController.navigate("Inicio_sesion") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = stringResource(R.string.iniciar_sesion))
        }

        //Google
        Boton_Personalizado(
            Modifier
                .clickable { /*TODO: Inicio sesión con Google Signin*/ }
                .padding(top = 8.dp)
        )

        val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
        Text(
            text = stringResource(R.string.crear_cuenta),
            color = textColor,
            modifier = Modifier.padding(top = 12.dp).clickable(onClick = {navHostController.navigate("Crear_cuenta")})
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
fun Boton_Personalizado(modifier: Modifier = Modifier) {
    val buttonShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(color = Color.White, shape = buttonShape)
            .clip(buttonShape)
            .border(2.dp, color = Color.Black, shape = buttonShape),
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