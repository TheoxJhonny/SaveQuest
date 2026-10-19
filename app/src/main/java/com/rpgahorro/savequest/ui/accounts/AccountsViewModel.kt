package com.rpgahorro.savequest.ui.accounts

import android.database.sqlite.SQLiteConstraintException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rpgahorro.savequest.data.repository.AccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsViewModel(private val repository: AccountRepository) : ViewModel() {
    val accounts = repository.accounts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var created by mutableStateOf(false)
        private set

    fun clearStatus() {
        if (!saving) {
            error = null; created = false
        }
    }

    fun create(name: String) {
        if (saving || created) return
        saving = true
        error = null
        viewModelScope.launch {
            try {
                repository.create(name)
                created = true
            } catch (e: CancellationException) {
                throw e
            } catch (_: SQLiteConstraintException) {
                error = "Ya existe una cuenta con ese nombre."
            } catch (e: IllegalArgumentException) {
                error = e.message
            } catch (_: Exception) {
                error = "No se pudo crear la cuenta. Inténtalo nuevamente."
            } finally {
                saving = false
            }
        }
    }

    class Factory(private val repository: AccountRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AccountsViewModel::class.java))
            return AccountsViewModel(repository) as T
        }
    }
}
