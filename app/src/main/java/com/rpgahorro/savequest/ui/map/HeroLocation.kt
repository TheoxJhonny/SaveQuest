package com.rpgahorro.savequest.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

internal class HeroLocationState {
    var location by mutableStateOf<Location?>(null)
    var locating by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var centerTarget by mutableStateOf<Location?>(null)
    var centerRequest by mutableIntStateOf(0)
    var pendingCenter = false
    fun accept(fix: Location) {
        if (!isFreshHeroFix(fix)) return
        val previous = location
        if (previous != null && isFreshHeroFix(previous, 10_000) && previous.accuracy < fix.accuracy) return
        location = Location(fix)
        locating = false
        message = if (fix.accuracy > 100) "Ubicación aproximada (±${fix.accuracy.toInt()} m)." else null
        if (pendingCenter) center()
    }
    fun center() {
        centerTarget = location?.let { Location(it) }
        centerRequest++
        pendingCenter = false
    }
}

internal fun isFreshHeroFix(fix: Location, maxAgeMs: Long = 30_000): Boolean {
    val age = (SystemClock.elapsedRealtimeNanos() - fix.elapsedRealtimeNanos) / 1_000_000
    return age in 0..maxAgeMs && fix.hasAccuracy() && fix.accuracy >= 0 &&
        fix.latitude.isFinite() && fix.longitude.isFinite() &&
        fix.latitude in -90.0..90.0 && fix.longitude in -180.0..180.0
}

/** Foreground-only updates. No location is written to Room or a file. */
@Composable
internal fun rememberHeroLocation(): Pair<HeroLocationState, () -> Unit> {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val manager = remember(context) { context.getSystemService(LocationManager::class.java) }
    val state = remember { HeroLocationState() }
    var refresh by remember { mutableIntStateOf(0) }
    var resumed by remember { mutableStateOf(false) }
    fun hasPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) { state.locating = true; refresh++ }
        else {
            state.pendingCenter = false
            state.locating = false
            state.message = "Permite la ubicación en los ajustes de la app para encontrar a tu héroe."
        }
    }
    DisposableEffect(owner, manager, refresh) {
        var subscribed = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) { state.accept(location) }
            override fun onProviderEnabled(provider: String) { refresh++ }
            override fun onProviderDisabled(provider: String) {
                state.location = null
                state.message = "Comprueba que la ubicación del teléfono esté activada."
                refresh++
            }
            @Deprecated("Required on Android 8")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        fun stop() {
            manager.removeUpdates(listener)
            subscribed = false
        }
        fun sync() {
            resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!resumed || !hasPermission()) {
                stop()
                state.location = null
                return
            }
            if (subscribed) return
            try {
                if (!LocationManagerCompat.isLocationEnabled(manager)) {
                    state.message = "Activa la ubicación del teléfono para encontrar a tu héroe."
                    state.locating = false
                    return
                }
                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                    .filter { (it != LocationManager.GPS_PROVIDER || fine) && manager.isProviderEnabled(it) }
                providers.forEach { provider ->
                    manager.requestLocationUpdates(provider, 3_000L, 3f, listener, Looper.getMainLooper())
                    manager.getLastKnownLocation(provider)?.takeIf { isFreshHeroFix(it, 15_000) }?.let(state::accept)
                }
                subscribed = providers.isNotEmpty()
                if (!subscribed) {
                    state.locating = false
                    state.message = "No hay un proveedor de ubicación disponible."
                }
            } catch (_: SecurityException) {
                stop()
                state.location = null
                state.locating = false
                state.message = "No hay permiso de ubicación. Puedes seguir explorando el mapa."
            }
        }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        owner.lifecycle.addObserver(observer)
        sync()
        onDispose { owner.lifecycle.removeObserver(observer); stop() }
    }
    LaunchedEffect(state.locating, refresh) {
        if (state.locating) {
            delay(25_000)
            state.locating = false
            state.pendingCenter = false
            state.message = "No se pudo obtener tu posición. Intenta otra vez en un lugar abierto."
        }
    }
    LaunchedEffect(resumed) {
        while (resumed) {
            delay(5_000)
            state.location?.let { if (!isFreshHeroFix(it)) {
                state.location = null
                state.message = "Se perdió la señal de ubicación. Pulsa para volver a buscar."
            } }
        }
    }
    val locate: () -> Unit = {
        if (!LocationManagerCompat.isLocationEnabled(manager)) {
            state.message = "Activa la ubicación y vuelve al mapa."
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            state.message = null
            state.pendingCenter = true
            if (hasPermission()) {
                if (state.location?.let { isFreshHeroFix(it, 15_000) } == true) state.center()
                else { state.locating = true; refresh++ }
            } else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    return state to locate
}
