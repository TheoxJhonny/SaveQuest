package com.rpgahorro.savequest.data.model

import org.json.JSONObject

enum class PlaceCategory(val key: String, val label: String, val expenseCategory: String) {
    GROCERIES("groceries", "Supermercados", "Alimentación"),
    FOOD("food", "Comida", "Comida"),
    PHARMACY("pharmacy", "Farmacias", "Salud"),
    SHOPPING("shopping", "Tiendas", "Compras");

    companion object {
        fun fromKey(key: String): PlaceCategory =
            entries.firstOrNull { it.key == key }
                ?: throw IllegalArgumentException("El tipo de comercio no es válido.")
    }
}

/** Snapshot del comercio: el historial no depende de futuras actualizaciones de OSM. */
data class MapPlace(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val address: String,
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(id.isNotBlank() && id.length <= 200) {
            "El identificador del comercio no es válido."
        }
        require(name.isNotBlank() && name.length <= 200) {
            "El nombre debe tener entre 1 y 200 caracteres."
        }
        require(address.length <= 500) { "La dirección es demasiado larga." }
        require(
            latitude.isFinite() &&
                longitude.isFinite() &&
                latitude in -90.0..90.0 &&
                longitude in -180.0..180.0
        ) {
            "La ubicación del comercio no es válida."
        }
    }

    fun toJson(): String =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("category", category.key)
            .put("address", address)
            .put("latitude", latitude)
            .put("longitude", longitude)
            .toString()

    companion object {
        fun fromJson(json: String): MapPlace = fromJson(JSONObject(json))

        fun fromJson(json: JSONObject): MapPlace =
            MapPlace(
                id = json.getString("id"),
                name = json.getString("name"),
                category = PlaceCategory.fromKey(json.getString("category")),
                address = json.getString("address"),
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
            )
    }
}
