package com.example.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class GpsLocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val address: String? = null,
    val isMock: Boolean = false
)

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): GpsLocationResult? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            val location = suspendCancellableCoroutine<Location?> { continuation ->
                val cancellationSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationSource.token
                ).addOnSuccessListener { loc ->
                    if (loc != null) {
                        continuation.resume(loc)
                    } else {
                        // Fallback to last known location
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                continuation.resume(lastLoc)
                            }
                            .addOnFailureListener {
                                continuation.resume(null)
                            }
                    }
                }.addOnFailureListener {
                    // Fallback to last known location on error
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLoc ->
                            continuation.resume(lastLoc)
                        }
                        .addOnFailureListener {
                            continuation.resume(null)
                        }
                }

                continuation.invokeOnCancellation {
                    cancellationSource.cancel()
                }
            }

            location?.let { loc ->
                val address = resolveAddress(loc.latitude, loc.longitude)
                GpsLocationResult(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracyMeters = loc.accuracy,
                    address = address
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun resolveAddress(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            val addr = addresses.firstOrNull()?.let {
                                (0..it.maxAddressLineIndex).joinToString(", ") { i -> it.getAddressLine(i) }
                            }
                            continuation.resume(addr)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    addresses?.firstOrNull()?.let {
                        (0..it.maxAddressLineIndex).joinToString(", ") { i -> it.getAddressLine(i) }
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        fun calculateDistanceMeters(
            startLat: Double,
            startLng: Double,
            endLat: Double,
            endLng: Double
        ): Float {
            val results = FloatArray(1)
            Location.distanceBetween(startLat, startLng, endLat, endLng, results)
            return results[0]
        }

        fun formatDistance(meters: Float): String {
            return if (meters < 1000) {
                "${meters.toInt()} m"
            } else {
                String.format(Locale.getDefault(), "%.1f km", meters / 1000f)
            }
        }
    }
}
