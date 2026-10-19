package com.rpgahorro.savequest.ui.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.data.repository.TransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class TransactionSaveState(
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {
    val transactions = repository.transactions
    var saveState by mutableStateOf(TransactionSaveState())
        private set

    fun clearStatus() {
        if (!saveState.saving) saveState = TransactionSaveState()
    }

    fun addTransaction(
        type: TransactionType,
        amount: Long,
        category: String,
        description: String,
        accountId: Long = 1,
        latitude: Double? = null,
        longitude: Double? = null,
        place: com.rpgahorro.savequest.data.model.MapPlace? = null,
    ) {
        if (saveState.saving || saveState.saved) return
        saveState = TransactionSaveState(saving = true)
        viewModelScope.launch {
            try {
                repository.record(
                    type,
                    amount,
                    category,
                    description,
                    accountId,
                    latitude,
                    longitude,
                    place,
                )
                saveState = TransactionSaveState(saved = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                saveState = TransactionSaveState(error = e.message)
            } catch (_: Exception) {
                saveState =
                    TransactionSaveState(
                        error = "No se pudo guardar el movimiento. Inténtalo nuevamente."
                    )
            }
        }
    }
}
