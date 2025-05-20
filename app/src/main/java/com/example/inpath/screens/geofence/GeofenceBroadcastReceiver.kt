package com.example.inpath.screens.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_GEOFENCE_TRANSITION_EVENT = "com.example.inpath.GEOFENCE_TRANSITION_EVENT"
    }
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e("GeofenceReceiver", "Evento de geofence nulo")
            Toast.makeText(context, "Evento de geofence nulo", Toast.LENGTH_LONG).show()
            return
        }
        if (geofencingEvent.hasError()) {
            val errorMessage = "Error en Geofence: ${geofencingEvent.errorCode}"
            Log.e("GeofenceReceiver", errorMessage)
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            return
        }

        val transitionType = geofencingEvent.geofenceTransition

        val mensaje = when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Mascota ha ENTRADO al área segura."
            Geofence.GEOFENCE_TRANSITION_DWELL -> "Mascota está PERMANECIENDO en el área segura."
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Mascota ha SALIDO del área segura."
            else -> "Transición desconocida"
        }

        Log.d("GeoFenceReceiver", "Geofence transition: $mensaje")

        GeoFenceSnackbarHelper.enqueue(context, mensaje)
    }

    object GeoFenceSnackbarHelper {
        private var snackbarQueue: MutableList<String> = mutableListOf()

        fun enqueue(context: Context, message: String) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            snackbarQueue.add(message)
        }
    }
}

