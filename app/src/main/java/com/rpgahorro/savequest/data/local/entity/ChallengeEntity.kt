package com.rpgahorro.savequest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class ChallengeEntity(

    @PrimaryKey
    val id: String,

    val title: String,

    val description: String,

    val target: Int,

    val progress: Int = 0,

    val xpReward: Int,

    val completed: Boolean = false,

    val rewardClaimed: Boolean = false
)