package com.rpgahorro.savequest.data.repository

import androidx.room.withTransaction
import com.rpgahorro.savequest.data.local.SaveQuestDatabase
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.data.model.TransactionType

class TransactionRepository(private val database: SaveQuestDatabase) {
    val transactions = database.transactionDao().getAll()

    /** El movimiento y su recompensa se confirman juntos o se revierten juntos. */
    suspend fun record(
        type: TransactionType,
        amount: Long,
        category: String,
        description: String,
        accountId: Long = 1,
        latitude: Double? = null,
        longitude: Double? = null,
        place: com.rpgahorro.savequest.data.model.MapPlace? = null,
    ) {
        require(type == TransactionType.INCOME || type == TransactionType.EXPENSE) {
            "Los aportes se registran desde Metas."
        }
        require(amount > 0) { "El monto debe ser mayor que cero." }
        require(category.isNotBlank()) { "Escribe una categoría." }
        require(
            (latitude == null && longitude == null) ||
                    (latitude != null &&
                            longitude != null &&
                            latitude.isFinite() &&
                            longitude.isFinite() &&
                            latitude in -90.0..90.0 &&
                            longitude in -180.0..180.0)
        ) {
            "La ubicación no es válida."
        }
        if (place != null) {
            require(type == TransactionType.EXPENSE) { "El comercio solo se vincula a gastos." }
            require(latitude == place.latitude && longitude == place.longitude) {
                "La ubicación no coincide con el comercio seleccionado."
            }
        }
        database.withTransaction {
            database
                .transactionDao()
                .insert(
                    TransactionEntity(
                        type = type.name,
                        amount = amount,
                        accountId = accountId,
                        category = category.trim(),
                        latitude = latitude,
                        longitude = longitude,
                        placeId = place?.id,
                        placeName = place?.name,
                        placeCategory = place?.category?.key,
                        placeAddress = place?.address,
                        description = description.trim(),
                        date = System.currentTimeMillis(),
                    )
                )
            awardMovement(database)
        }
    }
}
