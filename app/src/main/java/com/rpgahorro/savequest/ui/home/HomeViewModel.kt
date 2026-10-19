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
    challengeRepository: ChallengeRepository,
    savingGoalRepository: com.rpgahorro.savequest.data.repository.SavingGoalRepository,
    private val sharedExpenseRepository: com.rpgahorro.savequest.data.repository.SharedExpenseRepository
) : ViewModel() {

    val uiState = combine(
        transactionRepository.transactions,
        userProgressRepository.progress,
        challengeRepository.challenges,
        savingGoalRepository.goals,
        sharedExpenseRepository.expenses
    ) { transactions, progress, challenges, goals, shared ->

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

        val sharedExpenses = shared.sumOf { it.expense.ownExpense }
        val cash = totalIncome - totalExpenses - shared.sumOf { it.expense.cashOutflow }
        val receivable = shared.sumOf { it.receivable }
        val payable = shared.sumOf { it.payable }
        val totalXp = progress?.totalXp ?: 0

        val level = (totalXp / 100) + 1

        val currentLevelXp = totalXp % 100

        HomeUiState(
            balance = cash + receivable - payable,
            receivable = receivable,
            payable = payable,
            reserved = goals.sumOf { it.savedAmount },
            available = cash - goals.sumOf { it.savedAmount },
            goals = goals,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses + sharedExpenses,
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