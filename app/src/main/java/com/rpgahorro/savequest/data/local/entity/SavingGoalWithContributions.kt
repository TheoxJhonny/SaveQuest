package com.rpgahorro.savequest.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SavingGoalWithContributions(
    @Embedded val goal: SavingGoalEntity,
    @Relation(parentColumn = "id", entityColumn = "goalId")
    val contributions: List<GoalContributionEntity>
) {
    // El saldo se deriva del historial; no se guarda una segunda copia.
    val savedAmount: Long get() = contributions.sumOf { it.amount }
}
