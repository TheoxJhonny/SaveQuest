package com.rpgahorro.savequest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rpgahorro.savequest.data.repository.TransactionRepository
import com.rpgahorro.savequest.data.repository.UserProgressRepository
import com.rpgahorro.savequest.data.repository.ChallengeRepository

class HomeViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val userProgressRepository: UserProgressRepository,
    private val challengeRepository: ChallengeRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {

            return HomeViewModel(
                transactionRepository,
                userProgressRepository,
                challengeRepository
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}