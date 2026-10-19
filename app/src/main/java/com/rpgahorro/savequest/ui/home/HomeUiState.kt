package com.rpgahorro.savequest.ui.home

import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.data.local.entity.ChallengeEntity

data class HomeUiState(
    val receivable: Long = 0,
    val payable: Long = 0,
    val balance: Long = 0,
    val reserved: Long = 0,
    val available: Long = 0,
    val goals: List<com.rpgahorro.savequest.data.local.entity.SavingGoalWithContributions> = emptyList(),
    val totalIncome: Long = 0,
    val totalExpenses: Long = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),


    val totalXp: Int = 0,
    val level: Int = 1,
    val currentLevelXp: Int = 0,
    val xpForNextLevel: Int = 100,
    val challenges: List<ChallengeEntity> = emptyList()
)