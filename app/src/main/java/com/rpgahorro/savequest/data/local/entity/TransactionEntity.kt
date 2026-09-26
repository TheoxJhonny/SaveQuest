package com.rpgahorro.savequest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: String,

    val amount: Long,

    val category: String,

    val description: String,

    val date: Long,

    val goalId: Long? = null,

    val latitude: Double? = null,

    val longitude: Double? = null
)