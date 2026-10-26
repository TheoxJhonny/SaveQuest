package com.rpgahorro.savequest.ui.map

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.net.URL
import java.io.InputStream
import javax.net.ssl.HttpsURLConnection

/** At most one online attempt per view; no connectivity-driven reload loops. */
internal class HybridMapSession {
    var offlineLocked = false
        private set
    fun lockOffline(): Boolean {
        if (offlineLocked) return false
        offlineLocked = true
        return true
    }
}

/** Owns base style only. Callers restore dynamic sources after every style load. */
internal class HybridMapLoader(
    private val context: Context,
    private val view: MapView,
    private val map: MapLibreMap,
    private val onStyle: (Style) -> Unit,
    private val onReady: () -> Unit,
    private val onError: () -> Unit,
    private val started: Long = SystemClock.elapsedRealtime(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val session = HybridMapSession()
    private var online = false
    private var closed = false
    private var loaded = false
    private var ready = false
    private var generation = 0
    private var watchdog: Job? = null
    private var health: Job? = null
    private var template = ""
    private var endpoint = ""
    private val failedListener = MapView.OnDidFailLoadingMapListener {
        if (!closed) { if (online) fallback() else onError() }
    }
    private val renderedListener = MapView.OnDidFinishRenderingMapListener { fully ->
        if (!closed && loaded && fully) {
            watchdog?.cancel()
            if (!ready) {
                ready = true
                Log.i("SaveQuestMap", "first_full_frame_ms=${SystemClock.elapsedRealtime()-started} mode=${if (online) "online" else "offline"}")
                onReady()
            }
        }
    }
    private val cameraListener = MapLibreMap.OnCameraIdleListener { if (online) armWatchdog() }

    fun start(endpointOverride: String? = null, availability: (suspend () -> Boolean)? = null) {
        view.addOnDidFailLoadingMapListener(failedListener)
        view.addOnDidFinishRenderingMapListener(renderedListener)
        map.addOnCameraIdleListener(cameraListener)
        scope.launch {
            val config = withContext(Dispatchers.IO) {
                template = context.assets.open("map/style.json").bufferedReader().use { it.readText() }
                JSONObject(context.assets.open("map/hybrid-config.json").bufferedReader().use { it.readText() })
            }
            endpoint = endpointOverride ?: config.optString("onlinePmtilesUrl").trim()
            val available = endpoint.isNotEmpty() && (availability?.invoke() ?: withContext(Dispatchers.IO) { probe() })
            if (!available) session.lockOffline()
            load(available)
            if (available) health = scope.launch {
                while (isActive && online) {
                    delay(15_000)
                    if (!withContext(Dispatchers.IO) { probe(127) }) fallback()
                }
            }
        }.invokeOnCompletion { error ->
            if (error != null && error !is CancellationException && !closed) {
                view.post { if (!closed) onError() }
            }
        }
    }

    /** Require a real 206 response and matching archive header/root/metadata, not just connectivity. */
    private fun probe(bytes: Int = 16384): Boolean = runCatching {
        val url = URL(endpoint)
        require(url.protocol == "https" && url.userInfo == null && url.ref.isNullOrEmpty())
        val expected = context.assets.open("map/santiago.pmtiles").use { readBounded(it, bytes) }
        val connection = url.openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 2500
            connection.readTimeout = 2500
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Range", "bytes=0-${expected.size-1}")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.responseCode == 206 &&
                connection.getHeaderField("Content-Range")?.startsWith("bytes 0-${expected.size-1}/") == true &&
                connection.inputStream.use { readBounded(it, expected.size + 1) }.contentEquals(expected)
        } finally { connection.disconnect() }
    }.getOrDefault(false)

    private fun readBounded(input: InputStream, limit: Int): ByteArray {
        val buffer = ByteArray(limit)
        var offset = 0
        while (offset < limit) {
            val count = input.read(buffer, offset, limit - offset)
            if (count < 0) break
            offset += count
        }
        return buffer.copyOf(offset)
    }

    private fun armWatchdog() {
        watchdog?.cancel()
        watchdog = scope.launch { delay(12_000); if (online) fallback() }
    }

    private fun fallback() {
        if (closed || !session.lockOffline()) return
        health?.cancel()
        Log.i("SaveQuestMap", "fallback=offline")
        load(false)
    }

    private fun load(useOnline: Boolean) {
        online = useOnline
        loaded = false
        ready = false
        watchdog?.cancel()
        val token = ++generation
        val json = JSONObject(template)
        json.getJSONObject("sources").getJSONObject("santiago")
            .put("url", if (online) "pmtiles://$endpoint" else "pmtiles://asset://map/santiago.pmtiles")
        // setStyle keeps the existing MapView and camera; do not reset zoom/target here.
        map.setStyle(Style.Builder().fromJson(json.toString())) { style ->
            if (!closed && token == generation) {
                onStyle(style)
                loaded = true
            }
        }
        if (online) armWatchdog()
    }

    fun close() {
        closed = true
        scope.cancel()
        view.removeOnDidFailLoadingMapListener(failedListener)
        view.removeOnDidFinishRenderingMapListener(renderedListener)
        map.removeOnCameraIdleListener(cameraListener)
    }
}
