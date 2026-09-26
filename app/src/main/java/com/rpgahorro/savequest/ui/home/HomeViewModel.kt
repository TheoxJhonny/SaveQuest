package com.rpgahorro.savequest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.data.repository.TransactionRepository
import com.rpgahorro.savequest.data.repository.UserProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.rpgahorro.savequest.data.repository.ChallengeRepository

class HomeViewModel(
    transactionRepository: TransactionRepository,
    userProgressRepository: UserProgressRepository,
    challengeRepository: ChallengeRepository
) : ViewModel() {

    val uiState = combine(
        transactionRepository.transactions,
        userProgressRepository.progress,
        challengeRepository.challenges
    ) { transactions, progress, challenges ->

        val totalIncome = transactions
            .filter {
                it.type == TransactionType.INCOME.name
            }
            .sumOf {
                it.amount
            }

        val totalExpenses = transactions
            .filter {
                it.type == TransactionType.EXPENSE.name
            }
            .sumOf {
                it.amount
            }

        val totalXp = progress?.totalXp ?: 0

        val level = (totalXp / 100) + 1

        val currentLevelXp = totalXp % 100

        HomeUiState(
            balance = totalIncome - totalExpenses,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            recentTransactions = transactions.take(3),

            totalXp = totalXp,
            level = level,
            currentLevelXp = currentLevelXp,
            xpForNextLevel = 100,

            challenges = challenges


        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}