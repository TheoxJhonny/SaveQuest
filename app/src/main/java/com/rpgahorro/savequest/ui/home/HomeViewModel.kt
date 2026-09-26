package com.rpgahorro.savequest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    repository: TransactionRepository
) : ViewModel() {

    val uiState = repository.transactions
        .map { transactions ->

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

            HomeUiState(
                balance = totalIncome - totalExpenses,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                recentTransactions = transactions.take(3)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}