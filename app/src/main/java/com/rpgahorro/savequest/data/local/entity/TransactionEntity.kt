package com.rpgahorro.savequest.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys =
        [
            ForeignKey(
                entity = AccountEntity::class,
                parentColumns = ["id"],
                childColumns = ["accountId"],
                onDelete = ForeignKey.RESTRICT,
            )
        ],
    indices = [Index("accountId"), Index("placeId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val category: String,
    val description: String,
    val date: Long,
    val goalId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accountId: Long = 1,
    val placeId: String? = null,
    val placeName: String? = null,
    val placeCategory: String? = null,
    val placeAddress: String? = null,
)
