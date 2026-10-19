package com.rpgahorro.savequest.data.repository

import com.rpgahorro.savequest.data.local.dao.SavingGoalDao

class SavingGoalRepository(private val dao: SavingGoalDao) {
    val goals = dao.observeGoals()

    suspend fun createGoal(name: String, targetAmount: Long): Long =
        dao.createGoal(name, targetAmount)

    suspend fun deleteGoal(goalId: Long) = dao.deleteGoal(goalId)

    suspend fun contribute(goalId: Long, amount: Long, accountId: Long = 1): Long =
        dao.contribute(goalId, amount, accountId)
}
