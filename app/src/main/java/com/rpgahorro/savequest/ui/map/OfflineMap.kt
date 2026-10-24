package com.rpgahorro.savequest.ui.map

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource

data class MapPoint(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val expense: Boolean = false,
)

/** Todas las fuentes del estilo son assets locales; no existe dependencia de una caché. */
@Composable
fun OfflineMap(
    points: List<MapPoint>,
    modifier: Modifier = Modifier,
    onPoint: (List<Long>) -> Unit = {},
    onPlace: (Double, Double) -> Unit = { _, _ -> },
    onStreet: (String) -> Unit = {},
    onReady: () -> Unit = {},
    onError: () -> Unit = {},
    places: List<com.rpgahorro.savequest.data.model.MapPlace> = emptyList(),
    focusedPlace: com.rpgahorro.savequest.data.model.MapPlace? = null,
    onBusiness: (List<String>) -> Unit = {},
    onLongPress: (Double, Double) -> Unit = { _, _ -> },
    initialZoom: Double = 12.0,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val currentPoints by rememberUpdatedState(points)
    val businessClick by rememberUpdatedState(onBusiness)
    val longPress by rememberUpdatedState(onLongPress)
    var catalogJson by remember {
        mutableStateOf("{\"type\":\"FeatureCollection\",\"features\":[]}")
    }
    val currentCatalogJson by rememberUpdatedState(catalogJson)
    val pointClick by rememberUpdatedState(onPoint)
    val placeClick by rememberUpdatedState(onPlace)
    val streetClick by rememberUpdatedState(onStreet)
    val ready by rememberUpdatedState(onReady)
    val failed by rememberUpdatedState(onError)
    var latitude by rememberSaveable { mutableDoubleStateOf(-33.4489) }
    var longitude by rememberSaveable { mutableDoubleStateOf(-70.6693) }
    var zoom by rememberSaveable { mutableDoubleStateOf(initialZoom) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var disposed by remember { mutableStateOf(false) }
    val view =
        remember(context) {
            MapLibre.getInstance(context)
            MapView(context).apply { onCreate(null) }
        }
    DisposableEffect(view, owner) {
        var started = false
        var resumed = false
        fun sync() {
            val state = owner.lifecycle.currentState
            val shouldStart = state.isAtLeast(Lifecycle.State.STARTED)
            val shouldResume = state.isAtLeast(Lifecycle.State.RESUMED)
            if (resumed && !shouldResume) {
                view.onPause()
                resumed = false
            }
            if (started && !shouldStart) {
                view.onStop()
                started = false
            }
            if (!started && shouldStart) {
                view.onStart()
                started = true
            }
            if (!resumed && shouldResume) {
                view.onResume()
                resumed = true
            }
        }

        val observer = LifecycleEventObserver { _, _ -> sync() }
        owner.lifecycle.addObserver(observer)
        sync()
        val memory =
            object : android.content.ComponentCallbacks2 {
                override fun onConfigurationChanged(config: android.content.res.Configuration) =
                    Unit

                override fun onLowMemory() {
                    view.onLowMemory()
                }

                override fun onTrimMemory(level: Int) {
                    view.onLowMemory()
                }
            }
        context.registerComponentCallbacks(memory)
        val mapStarted = android.os.SystemClock.elapsedRealtime()
        var styleLoaded = false
        var readySent = false
        view.addOnDidFinishRenderingMapListener { fully ->
            if (fully && styleLoaded && !readySent && !disposed) {
                readySent = true
                android.util.Log.i("SaveQuestMap", "baseline_first_full_frame_ms=" + (android.os.SystemClock.elapsedRealtime() - mapStarted))
                ready()
            }
        }
        view.addOnDidFailLoadingMapListener { if (!disposed) failed() }
        view.getMapAsync { controller ->
            if (!disposed) {
                map = controller
                controller.setMinZoomPreference(10.0)
                controller.setMaxZoomPreference(18.0)
                controller.setLatLngBoundsForCameraTarget(
                    LatLngBounds.from(-33.25, -70.45, -33.65, -70.85)
                )
                controller.cameraPosition =
                    CameraPosition.Builder().target(LatLng(latitude, longitude)).zoom(zoom).build()
                controller.uiSettings.isRotateGesturesEnabled = false
                controller.uiSettings.isTiltGesturesEnabled = false
                // Atribución visible permanente en el contenedor Compose.
                controller.uiSettings.isAttributionEnabled = false
                controller.uiSettings.isLogoEnabled = false
                controller.addOnCameraIdleListener {
                    controller.cameraPosition.let { camera ->
                        camera.target?.let {
                            latitude = it.latitude
                            longitude = it.longitude
                        }
                        zoom = camera.zoom
                    }
                }
                controller.addOnMapLongClickListener { coordinate ->
                    longPress(coordinate.latitude, coordinate.longitude)
                    true
                }
                controller.addOnMapClickListener { coordinate ->
                    val screen = controller.projection.toScreenLocation(coordinate)
                    val hitbox =
                        android.graphics.RectF(
                            screen.x - 24,
                            screen.y - 24,
                            screen.x + 24,
                            screen.y + 24,
                        )
                    val hits = controller.queryRenderedFeatures(hitbox, "movements")
                    if (hits.isNotEmpty())
                        pointClick(hits.map { it.getNumberProperty("id").toLong() }.distinct())
                    else if (
                        controller.queryRenderedFeatures(hitbox, "places-icons").isNotEmpty()
                    ) {
                        val businesses = controller.queryRenderedFeatures(hitbox, "places-icons")
                        businessClick(businesses.map { it.getStringProperty("id") }.distinct())
                        placeClick(coordinate.latitude, coordinate.longitude)
                    } else {
                        val roads = controller.queryRenderedFeatures(hitbox, "roads")
                        streetClick(roads.firstOrNull()?.getStringProperty("name").orEmpty())
                        placeClick(coordinate.latitude, coordinate.longitude)
                    }
                    true
                }
                controller.setStyle(Style.Builder().fromUri("asset://map/style.json")) { style ->
                    if (!disposed) {
                        style
                            .getSourceAs<GeoJsonSource>("movements")
                            ?.setGeoJson(pointsJson(currentPoints))
                        com.rpgahorro.savequest.data.model.PlaceCategory.entries.forEach { category
                            ->
                            style.addImage("poi-${category.key}", placeIcon(category))
                        }
                        style.getSourceAs<GeoJsonSource>("places")?.setGeoJson(currentCatalogJson)
                        styleLoaded = true
                    }
                }
            }
        }
        onDispose {
            disposed = true
            owner.lifecycle.removeObserver(observer)
            context.unregisterComponentCallbacks(memory)
            if (resumed) view.onPause()
            if (started) view.onStop()
            view.onDestroy()
            map = null
        }
    }
    LaunchedEffect(points, map) {
        map?.getStyle {
            if (!disposed)
                it.getSourceAs<GeoJsonSource>("movements")?.setGeoJson(pointsJson(currentPoints))
        }
    }
    LaunchedEffect(places) {
        catalogJson =
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                placesJson(places)
            }
    }
    LaunchedEffect(catalogJson, map) {
        map?.getStyle {
            if (!disposed) it.getSourceAs<GeoJsonSource>("places")?.setGeoJson(currentCatalogJson)
        }
    }
    LaunchedEffect(focusedPlace, map) {
        focusedPlace?.let { place ->
            map?.animateCamera(
                org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                    LatLng(place.latitude, place.longitude),
                    16.5,
                )
            )
        }
    }
    AndroidView(factory = { view }, modifier = modifier)
}

private fun pointsJson(points: List<MapPoint>): String =
    JSONObject()
        .put("type", "FeatureCollection")
        .put(
            "features",
            JSONArray().apply {
                points.forEach { point ->
                    put(
                        JSONObject()
                            .put("type", "Feature")
                            .put(
                                "properties",
                                JSONObject().put("id", point.id).put("expense", point.expense),
                            )
                            .put(
                                "geometry",
                                JSONObject()
                                    .put("type", "Point")
                                    .put(
                                        "coordinates",
                                        JSONArray().put(point.longitude).put(point.latitude),
                                    ),
                            )
                    )
                }
            },
        )
        .toString()
