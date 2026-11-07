package com.example.currencywidget.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.example.currencywidget.data.repository.WeatherStore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Coarse position for the "where I am" weather place. Only used while the app is open
 * (no background location permission, no location updates kept running).
 */
object LocationHelper {

    fun hasPermission(context: Context) = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /** Stores the phone's approximate position. Returns true if a position was found. */
    @SuppressLint("MissingPermission")
    suspend fun updateCurrentPosition(context: Context): Boolean {
        if (!hasPermission(context)) return false
        val lm = context.getSystemService(LocationManager::class.java) ?: return false
        val providers = lm.getProviders(true)

        // A recent cached fix is good enough for weather (~city level) and costs nothing.
        val cached = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        val fresh = if (cached != null && System.currentTimeMillis() - cached.time < 60 * 60 * 1000L) cached
        else requestSingleFix(lm, providers) ?: cached

        fresh ?: return false
        WeatherStore.getInstance(context).saveCurrentPosition(fresh.latitude, fresh.longitude)
        return true
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFix(lm: LocationManager, providers: List<String>): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val provider = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && LocationManager.FUSED_PROVIDER in providers -> LocationManager.FUSED_PROVIDER
            LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                lm.getCurrentLocation(provider, signal, { it.run() }) { location -> cont.resume(location) }
            }
        }
    }
}
