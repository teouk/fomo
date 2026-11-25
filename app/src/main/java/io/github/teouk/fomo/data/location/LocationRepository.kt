package io.github.teouk.fomo.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** A point on the map. */
data class GeoPoint(val latitude: Double, val longitude: Double) {
    /**
     * Roughly inside Great Britain and Northern Ireland, the area Data Thistle covers.
     * A box is enough here: it only decides whether searching near this point can work.
     */
    val isInUk: Boolean
        get() = latitude in UK_MIN_LAT..UK_MAX_LAT && longitude in UK_MIN_LON..UK_MAX_LON

    private companion object {
        const val UK_MIN_LAT = 49.8
        const val UK_MAX_LAT = 60.9
        const val UK_MIN_LON = -8.7
        const val UK_MAX_LON = 1.8
    }
}

/**
 * The phone's current position, from Google Play services' Fused Location Provider
 * (it combines GPS, Wi-Fi and mobile networks). On the emulator, set the position with
 * Extended Controls → Location.
 *
 * Only approximate location is used: enough to find events nearby, and less personal
 * data than the app needs is never requested.
 */
class LocationRepository(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** The current position, or null without permission, without Play services, or if it fails. */
    @SuppressLint("MissingPermission") // checked by hasPermission() first
    suspend fun currentLocation(): GeoPoint? {
        if (!hasPermission()) return null
        // Ask for a recent fix first. If none arrives in time (common on the emulator,
        // which only sends a position when you press "Set location"), use the last known
        // one. With only the approximate permission, Android blurs the result to about 2 km.
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MS)
            .setDurationMillis(LOCATION_TIMEOUT_MS)
            .build()
        return try {
            val location = client.getCurrentLocation(request, CancellationTokenSource().token).await()
                ?: client.lastLocation.await()
            location?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No Play services, location switched off, or a timeout: fall back gracefully.
            null
        }
    }

    private companion object {
        const val MAX_LOCATION_AGE_MS = 5 * 60_000L
        const val LOCATION_TIMEOUT_MS = 5_000L
    }
}
