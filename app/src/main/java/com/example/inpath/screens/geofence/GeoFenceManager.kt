package com.example.inpath.screens.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

class GeoFenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun agregarGeofence(
        id: String,
        latitud: Double,
        longitud: Double,
        radioUsuario: Float
    ) {
        val radioFinal = radioUsuario + 50f

        val geofence = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitud, longitud, radioFinal)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_EXIT or
                        Geofence.GEOFENCE_TRANSITION_DWELL
            )
            .setLoiteringDelay(10000)
            .setNotificationResponsiveness(5000)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                    ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
        ) {
            Log.e("GeoFenceManager", "Permisos de ubicación no concedidos")
            return
        }

        // 👇 Elimina antes las geofences anteriores
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnCompleteListener {
                Log.d("GeoFenceManager", "Geofences anteriores eliminadas (si existían)")

                // 👇 Ahora sí, añade la nueva geofence
                geofencingClient.addGeofences(request, geofencePendingIntent)
                    .addOnSuccessListener {
                        Log.d("GeoFenceManager", "Geofence añadida correctamente")
                    }
                    .addOnFailureListener {
                        Log.e("GeoFenceManager", "Error al añadir geofence: ${it.message}")
                    }
            }
    }

    fun eliminarGeofence(id: String) {
        geofencingClient.removeGeofences(listOf(id))
            .addOnCompleteListener { task: Task<Void?> ->
                if (task.isSuccessful) {
                    Log.d("GeoFenceManager", "Geocerca eliminada correctamente: $id")
                } else {
                    val errorMessage = "Error al eliminar geocerca '$id': ${task.exception?.message}"
                    Log.e("GeoFenceManager", errorMessage)
                }
            }
    }
}
