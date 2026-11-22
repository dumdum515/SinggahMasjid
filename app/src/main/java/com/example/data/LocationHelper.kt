package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException

data class DeviceCoordinate(
    val lat: Double,
    val lon: Double,
    val label: String
)

class LocationHelper(context: Context) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): DeviceCoordinate? {
        return try {
            val tokenSource = CancellationTokenSource()
            val fresh: Location? = fusedClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
                .awaitTask()
            val location = fresh ?: fusedClient.lastLocation.awaitTask()
            if (location != null) {
                DeviceCoordinate(
                    lat = location.latitude,
                    lon = location.longitude,
                    label = "Lokasi GPS Aktif"
                )
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
