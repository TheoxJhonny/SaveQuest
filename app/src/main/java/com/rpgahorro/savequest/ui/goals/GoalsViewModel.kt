package com.rpgahorro.savequest.ui.goals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rpgahorro.savequest.data.repository.SavingGoalRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(private val repository: SavingGoalRepository) : ViewModel() {
    val goals = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun clearError() { error = null }

    fun create(name: String, amount: String, onSuccess: () -> Unit) = save(onSuccess) {
        repository.createGoal(name, parseAmount(amount))
    }

    fun contribute(goalId: Long, amount: String, onSuccess: () -> Unit) = save(onSuccess) {
        repository.contribute(goalId, parseAmount(amount))
    }

    private fun parseAmount(text: String): Long {
        require(text.isNotEmpty() && text.all { it in '0'..'9' }) { "Escribe un monto en pesos, sin puntos ni comas." }
        return requireNotNull(text.toLongOrNull()?.takeIf { it > 0 }) { "Escribe un monto válido mayor que cero." }
    }

    private fun save(onSuccess: () -> Unit, operation: suspend () -> Unit) {
        if (saving) return
        saving = true
        error = null
        viewModelScope.launch {
            try {
                operation()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                error = e.message
            } catch (_: Exception) {
                error = "No se pudo guardar. Inténtalo nuevamente."
            } finally { saving = false }
        }
    }

    class Factory(private val repository: SavingGoalRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GoalsViewModel::class.java))
            return GoalsViewModel(repository) as T
        }
    }
}
