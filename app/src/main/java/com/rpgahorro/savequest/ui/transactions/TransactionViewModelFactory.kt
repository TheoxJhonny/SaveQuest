package com.rpgahorro.savequest.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rpgahorro.savequest.data.repository.TransactionRepository
import com.rpgahorro.savequest.data.repository.UserProgressRepository

class TransactionViewModelFactory(
    private val repository: TransactionRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            return TransactionViewModel(
                repository,
                userProgressRepository
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}