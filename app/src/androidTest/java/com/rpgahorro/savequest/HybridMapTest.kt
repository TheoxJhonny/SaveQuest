package com.rpgahorro.savequest

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.ui.map.HybridMapLoader
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.style.sources.GeoJsonSource
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

class HybridMapTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unavailableOnlineFallsBackOncePreservingCameraAndDynamicSources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val offlineReady = AtomicBoolean(false)
        val offlineLoads = AtomicInteger(0)
        val attempts = AtomicInteger(0)
        var view: MapView? = null
        var loader: HybridMapLoader? = null
        val point = """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"id":42,"expense":true},"geometry":{"type":"Point","coordinates":[-70.6693,-33.4489]}}]}"""
        compose.setContent {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = {
                MapLibre.getInstance(it)
                MapView(it).also { v ->
                    view = v
                    v.onCreate(null); v.onStart(); v.onResume()
                    v.getMapAsync { map ->
                        map.cameraPosition = CameraPosition.Builder().target(LatLng(-33.4489,-70.6693)).zoom(15.0).build()
                        loader = HybridMapLoader(context, v, map,
                            onStyle = { style ->
                                style.getSourceAs<GeoJsonSource>("movements")!!.setGeoJson(point)
                                style.getSourceAs<GeoJsonSource>("places")!!.setGeoJson("""{"type":"FeatureCollection","features":[]}""")
                                if (org.json.JSONObject(style.json).getJSONObject("sources").getJSONObject("santiago").getString("url").startsWith("pmtiles://asset://")) offlineLoads.incrementAndGet()
                            },
                            onReady = {
                                if (offlineLoads.get() > 0) {
                                    assertEquals(15.0, map.cameraPosition.zoom, 0.001)
                                    assertEquals(-33.4489, map.cameraPosition.target!!.latitude, 0.00001)
                                    assertEquals(-70.6693, map.cameraPosition.target!!.longitude, 0.00001)
                                    val screen = map.projection.toScreenLocation(LatLng(-33.4489,-70.6693))
                                    assertTrue(map.queryRenderedFeatures(screen, "movements").isNotEmpty())
                                    assertNotNull(map.style!!.getSource("places"))
                                    offlineReady.set(true)
                                }
                            }, onError = { fail("Local PMTiles failed") },
                        ).also { l ->
                            // Simulate a host that passed the probe then failed during tile loading.
                            l.start("https://127.0.0.1:1/unavailable.pmtiles") { attempts.incrementAndGet(); true }
                        }
                    }
                }
            })
            DisposableEffect(Unit) {
                onDispose { loader?.close(); view?.onPause(); view?.onStop(); view?.onDestroy() }
            }
        }
        compose.waitUntil(40_000) { offlineReady.get() }
        // Past another watchdog interval: a failed source must never be selected again.
        Thread.sleep(13_000)
        assertEquals(1, offlineLoads.get())
        assertEquals(1, attempts.get())
    }
}
