package com.rpgahorro.savequest.ui.map

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.rpgahorro.savequest.ui.theme.*
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

/** Mapa híbrido con posición del héroe y fuentes dinámicas independientes. */
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
    val (hero, locateHero) = rememberHeroLocation()
    val heroJson = remember(hero.location) { heroPositionJson(hero.location) }
    val currentHeroJson by rememberUpdatedState(heroJson)
    val movementJson = remember(points) { pointsJson(points) }
    val currentMovementJson by rememberUpdatedState(movementJson)
    val appliedSources = remember { AppliedMapSources() }
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
        disposed = false
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
        var loader: HybridMapLoader? = null
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
                loader = HybridMapLoader(context, view, controller,
                    onStyle = { style ->
                        appliedSources.reset()
                        style.addImage("user-hero", heroLocationIcon())
                        style.getSourceAs<GeoJsonSource>("user-location")?.setGeoJson(currentHeroJson)
                        com.rpgahorro.savequest.data.model.PlaceCategory.entries.forEach { category ->
                            style.addImage("poi-${category.key}", placeIcon(category))
                        }
                        appliedSources.movements(style, currentMovementJson)
                        appliedSources.places(style, currentCatalogJson)
                    },
                    onReady = { ready() },
                    onError = { failed() },
                    started = mapStarted,
                ).also { it.start() }
            }
        }
        onDispose {
            disposed = true
            loader?.close()
            owner.lifecycle.removeObserver(observer)
            context.unregisterComponentCallbacks(memory)
            if (resumed) view.onPause()
            if (started) view.onStop()
            view.onDestroy()
            map = null
        }
    }
    LaunchedEffect(movementJson, map) {
        map?.getStyle {
            if (!disposed)
                appliedSources.movements(it, currentMovementJson)
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
            if (!disposed) appliedSources.places(it, currentCatalogJson)
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
    LaunchedEffect(heroJson, map) {
        map?.getStyle { style ->
            if (!disposed) style.getSourceAs<GeoJsonSource>("user-location")?.setGeoJson(currentHeroJson)
        }
    }
    LaunchedEffect(hero.centerRequest, map) {
        val target = hero.centerTarget
        if (target != null && isFreshHeroFix(target)) {
            // Real coordinates must never be clamped to the Santiago coverage boundary.
            if (target.latitude !in -33.65..-33.25 || target.longitude !in -70.85..-70.45)
                map?.setLatLngBoundsForCameraTarget(null)
            map?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                LatLng(target.latitude, target.longitude), 16.5))
        }
    }
    Box(modifier) {
        AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.BottomEnd).padding(12.dp),
            horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val outside = hero.location?.let { it.latitude !in -33.65..-33.25 || it.longitude !in -70.85..-70.45 } == true
            val message = if (outside) "Tu posición está fuera de la cartografía local de Santiago." else hero.message
            message?.let {
                Surface(color = SqSurface, shape = MaterialTheme.shapes.small, modifier = Modifier.widthIn(max = 260.dp)) {
                    Text(it, Modifier.padding(10.dp), color = SqTextPrimary, style = MaterialTheme.typography.bodySmall)
                }
            }
            SmallFloatingActionButton(onClick = { if (!hero.locating) locateHero() },
                containerColor = SqPrimary, contentColor = SqTextPrimary,
                modifier = Modifier.size(48.dp).testTag("recenter-hero")) {
                if (hero.locating) CircularProgressIndicator(Modifier.size(22.dp), color = SqTextPrimary, strokeWidth = 2.dp)
                else Icon(Icons.Default.MyLocation, contentDescription = "Volver a mi ubicación")
            }
        }
    }
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

/** Compare serialized content as well as list equality to avoid redundant native GeoJSON parsing. */
private class AppliedMapSources {
    private var movements: String? = null
    private var places: String? = null
    fun reset() { movements = null; places = null }
    fun movements(style: Style, json: String) {
        if (movements == json) return
        style.getSourceAs<GeoJsonSource>("movements")?.let { it.setGeoJson(json); movements = json }
    }
    fun places(style: Style, json: String) {
        if (places == json) return
        style.getSourceAs<GeoJsonSource>("places")?.let { it.setGeoJson(json); places = json }
    }
}

private fun heroPositionJson(location: android.location.Location?): String =
    JSONObject().put("type", "FeatureCollection").put("features", JSONArray().apply {
        if (location != null) put(JSONObject().put("type", "Feature")
            .put("properties", JSONObject().put("accuracy", location.accuracy.toDouble()))
            .put("geometry", JSONObject().put("type", "Point").put("coordinates",
                JSONArray().put(location.longitude).put(location.latitude))))
    }).toString()
