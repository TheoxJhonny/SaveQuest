package com.rpgahorro.savequest.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Coordenadas elegidas por la persona; no se hace seguimiento en segundo plano. */
@Composable
fun MovementLocation(latitude: Double?, longitude: Double?, enabled: Boolean, onChange: (Double?, Double?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var choosing by rememberSaveable { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var locationJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(enabled) { if (!enabled) locationJob?.cancel() }
    var message by remember { mutableStateOf<String?>(null) }
    val currentChange by rememberUpdatedState(onChange)
    fun locate() {
        if (locating || !enabled) return
        locating = true
        message = null
        locationJob = scope.launch {
            try {
                val manager = context.getSystemService(LocationManager::class.java)
                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val provider = if (fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER
                    else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else null
                if (provider == null) message = "Activa la ubicación del teléfono o elige un punto en el mapa."
                else {
                    val location = withTimeoutOrNull(25_000) {
                        suspendCancellableCoroutine<android.location.Location?> { continuation ->
                            val signal = CancellationSignal()
                            continuation.invokeOnCancellation { signal.cancel() }
                            LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) {
                                if (continuation.isActive) continuation.resume(it)
                            }
                        }
                    }
                    if (location == null) message = "No se obtuvo una ubicación. Puedes elegirla en el mapa."
                    else {
                        currentChange(location.latitude, location.longitude)
                        message = "Ubicación aproximada: margen de ${location.accuracy.toInt()} m."
                    }
                }
            } catch (_: SecurityException) {
                message = "No hay permiso de ubicación. Puedes elegirla en el mapa."
            } finally { locating = false }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) locate()
        else message = "Sin permiso: el movimiento puede guardarse sin ubicación."
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Ubicación (opcional)", style = MaterialTheme.typography.titleSmall)
        Text(if (latitude != null && longitude != null) String.format(Locale.ROOT, "%.5f, %.5f", latitude, longitude) else "Sin ubicación")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ choosing = true }, enabled = enabled && !locating) { Text("Elegir en mapa") }
            TextButton({
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
                else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, enabled = enabled && !locating) { Text(if (locating) "Buscando…" else "Usar GPS") }
        }
        if (latitude != null) TextButton({ onChange(null, null); message = null }, enabled = enabled && !locating) { Text("Quitar ubicación") }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
    if (choosing) {
        var draftLat by rememberSaveable { mutableStateOf(latitude) }
        var draftLon by rememberSaveable { mutableStateOf(longitude) }
        var ready by remember { mutableStateOf(false) }
        Dialog(onDismissRequest = { choosing = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(12.dp), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Elige el lugar", style = MaterialTheme.typography.titleLarge)
                    Text("Toca el mapa para situar tu movimiento.")
                    OfflineMap(if (draftLat != null && draftLon != null) listOf(MapPoint(0, draftLat!!, draftLon!!)) else emptyList(),
                        Modifier.weight(1f).fillMaxWidth(), onPlace = { lat, lon -> draftLat = lat; draftLon = lon },
                        onReady = { ready = true })
                    MapAttribution()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton({ choosing = false }) { Text("Cancelar") }
                        Button({ onChange(draftLat, draftLon); choosing = false }, enabled = ready && draftLat != null) { Text("Usar este lugar") }
                    }
                }
            }
        }
    }
}
