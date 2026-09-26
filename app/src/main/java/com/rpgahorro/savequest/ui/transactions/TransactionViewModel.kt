package com.rpgahorro.savequest.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    val transactions: Flow<List<TransactionEntity>> =
        repository.transactions

    fun addTransaction(
        type: TransactionType,
        amount: Long,
        category: String,
        description: String
    ) {

        val transaction = TransactionEntity(
            type = type.name,
            amount = amount,
            category = category,
            description = description,
            date = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.insert(transaction)
        }
    }
}