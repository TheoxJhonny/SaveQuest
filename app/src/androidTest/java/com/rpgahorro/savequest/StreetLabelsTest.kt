package com.rpgahorro.savequest

import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.ui.map.*
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class StreetLabelsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mainRoadNamesAtCityZoomAndLocalNamesWhenClose() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ready = AtomicBoolean(false)
        var view: MapView? = null
        var controller: MapLibreMap? = null
        var loader: HybridMapLoader? = null
        compose.setContent {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = {
                MapLibre.getInstance(it)
                MapView(it).also { v ->
                    view = v
                    v.onCreate(null); v.onStart(); v.onResume()
                    v.getMapAsync { map ->
                        controller = map
                        map.cameraPosition = CameraPosition.Builder().target(LatLng(-33.4489,-70.6693)).zoom(12.0).build()
                        loader = HybridMapLoader(context,v,map,
                            onStyle = { it.addImage("user-hero", heroLocationIcon()) },
                            onReady = { ready.set(true) }, onError = { fail("Map did not load") }
                        ).also { it.start() }
                    }
                }
            })
            DisposableEffect(Unit) {
                onDispose { loader?.close(); view?.onPause(); view?.onStop(); view?.onDestroy() }
            }
        }
        compose.waitUntil(60_000) { ready.get() }
        fun count(layer: String): Int {
            var result = 0
            compose.runOnIdle {
                val v = view!!
                result = controller!!.queryRenderedFeatures(android.graphics.RectF(0f,0f,v.width.toFloat(),v.height.toFloat()),layer).size
            }
            return result
        }
        compose.waitUntil(15_000) { count("major-road-labels") > 0 }
        assertEquals(0,count("road-labels"))
        compose.runOnIdle {
            controller!!.cameraPosition = CameraPosition.Builder().target(LatLng(-33.4489,-70.6693)).zoom(16.5).build()
        }
        compose.waitUntil(20_000) { count("road-labels") > 0 }
    }
}
