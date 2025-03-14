package com.example.inpath.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.inpath.R

@Composable
fun Inicio_sesion(navController: NavController){
    Column(){
        Row(){

        }
        Row(){
            Text(
                text = stringResource(R.string.usuario)
            )
        }
        Row(){

        }
        Row(){
            Text(
                text = stringResource(R.string.contraseña)
            )
        }
        Row(){

        }
        Row(){
            Text(
                text = stringResource(R.string.crear_cuenta)
            )
            Text(
                text = stringResource(R.string.crear_cuenta_hiper)
            )
        }

        Row(){
            Text(
                text = " " + stringResource(R.string.recordar_contraseña)
            )
            Text(
                text = " " + stringResource(R.string.recordar_contraseña_hiper)
            )
        }
    }
}
