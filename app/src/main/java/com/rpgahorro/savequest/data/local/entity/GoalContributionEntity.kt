package com.rpgahorro.savequest.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_contributions",
    foreignKeys = [ForeignKey(
        entity = SavingGoalEntity::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("goalId")]
)
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Long,
    val date: Long = System.currentTimeMillis()
)
