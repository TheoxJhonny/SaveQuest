package com.rpgahorro.savequest.ui.map

import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.data.model.MapPlace
import com.rpgahorro.savequest.data.model.PlaceCategory
import org.json.JSONArray
import org.json.JSONObject

fun TransactionEntity.savedPlace(): MapPlace? {
    if (
        placeId == null ||
            placeName == null ||
            placeCategory == null ||
            latitude == null ||
            longitude == null
    )
        return null
    return MapPlace(
        placeId,
        placeName,
        PlaceCategory.fromKey(placeCategory),
        placeAddress.orEmpty(),
        latitude,
        longitude,
    )
}

internal fun placesJson(places: List<MapPlace>): String =
    JSONObject()
        .put("type", "FeatureCollection")
        .put(
            "features",
            JSONArray().apply {
                places.forEach { place ->
                    put(
                        JSONObject()
                            .put("type", "Feature")
                            .put(
                                "properties",
                                JSONObject()
                                    .put("id", place.id)
                                    .put("name", place.name)
                                    .put("category", place.category.key),
                            )
                            .put(
                                "geometry",
                                JSONObject()
                                    .put("type", "Point")
                                    .put(
                                        "coordinates",
                                        JSONArray().put(place.longitude).put(place.latitude),
                                    ),
                            )
                    )
                }
            },
        )
        .toString()
