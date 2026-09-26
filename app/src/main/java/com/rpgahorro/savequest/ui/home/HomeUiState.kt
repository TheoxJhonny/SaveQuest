package com.rpgahorro.savequest.ui.home

import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.data.local.entity.ChallengeEntity

data class HomeUiState(
    val balance: Long = 0,
    val totalIncome: Long = 0,
    val totalExpenses: Long = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),


    val totalXp: Int = 0,
    val level: Int = 1,
    val currentLevelXp: Int = 0,
    val xpForNextLevel: Int = 100,
    val challenges: List<ChallengeEntity> = emptyList()
)