package com.smartmaps.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import com.smartmaps.app.data.remote.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.math.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    // Only update reverse-geocode when the user moves more than this (metres)
    private val GEOCODE_MIN_DISTANCE_M = 100.0
    private var lastGeocodedLocation: LatLng? = null

    /** Returns true if location has changed enough to warrant a new Nominatim call. */
    fun hasMovedSignificantly(newLocation: LatLng): Boolean {
        val last = lastGeocodedLocation ?: return true
        return haversine(last, newLocation) > GEOCODE_MIN_DISTANCE_M
    }

    fun markGeocodedAt(location: LatLng) {
        lastGeocodedLocation = location
    }

    private fun haversine(a: LatLng, b: LatLng): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lng - a.lng)
        val sinDLat = sin(dLat / 2)
        val sinDLon = sin(dLon / 2)
        val sinVal = sinDLat * sinDLat +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sinDLon * sinDLon
        return r * 2 * asin(sqrt(sinVal))
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(): Flow<LatLng> = callbackFlow {
        // Update position every 30 s, but only if moved ≥ 20 m
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30_000L)
            .setMinUpdateIntervalMillis(15_000L)
            .setMinUpdateDistanceMeters(20f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    _currentLocation.value = latLng
                    trySend(latLng)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): LatLng? {
        return try {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            val latLng = LatLng(location.latitude, location.longitude)
                            _currentLocation.value = latLng
                            continuation.resume(latLng)
                        } else {
                            continuation.resume(null)
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            }
        } catch (e: Exception) {
            null
        }
    }
}
