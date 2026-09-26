package com.rpgahorro.savequest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgressEntity(

    @PrimaryKey
    val id: Int = 1,

    val totalXp: Int = 0
)