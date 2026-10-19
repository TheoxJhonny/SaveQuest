package com.rpgahorro.savequest.data.repository

import android.content.res.AssetManager
import com.rpgahorro.savequest.data.model.MapPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** Catálogo inmutable, cargado una sola vez fuera del hilo de interfaz. */
class PlaceCatalog(private val assets: AssetManager) {
    private val mutex = Mutex()
    private var cached: List<MapPlace>? = null

    suspend fun load(): List<MapPlace> =
        mutex.withLock {
            cached
                ?: withContext(Dispatchers.IO) {
                    val json =
                        assets.open("map/places.json").bufferedReader().use {
                            JSONArray(it.readText())
                        }
                    List(json.length()) { MapPlace.fromJson(json.getJSONObject(it)) }
                        .also { places ->
                            require(places.map { it.id }.distinct().size == places.size) {
                                "El catálogo contiene IDs repetidos."
                            }
                            cached = places
                        }
                }
        }
}
