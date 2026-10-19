package com.rpgahorro.savequest.ui.shared

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.rpgahorro.savequest.data.repository.*
import com.rpgahorro.savequest.domain.shared.SharedItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SharedExpensesViewModel(private val repository: SharedExpenseRepository,
    contacts: ContactRepository, accounts: AccountRepository) : ViewModel() {
    var error by mutableStateOf<String?>(null); private set
    var loadError by mutableStateOf<String?>(null); private set
    var saving by mutableStateOf(false); private set
    var saved by mutableStateOf(false); private set
    val expenses = repository.expenses.catch { loadError = "No se pudieron cargar los gastos. Vuelve a abrir esta pantalla." }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val contacts = contacts.contacts.catch { loadError = "No se pudieron cargar los contactos." }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val accounts = accounts.accounts.catch { loadError = "No se pudieron cargar las cuentas." }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun clearStatus() { error = null; saved = false }
    fun save(id: String, title: String, items: List<SharedItem>, participants: List<Long>, payer: Long,
        account: Long?, shares: Map<Long, Long>?) {
        if (saving || saved) return
        saving = true; error = null
        viewModelScope.launch {
            try { repository.record(id, title, items, participants, payer, account, shares); saved = true }
            catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) { error = e.message }
            catch (_: Exception) { error = "No se guardó el gasto. Puedes volver a intentarlo." }
            finally { saving = false }
        }
    }

    class Factory(private val repository: SharedExpenseRepository, private val contacts: ContactRepository,
        private val accounts: AccountRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SharedExpensesViewModel::class.java))
            return SharedExpensesViewModel(repository, contacts, accounts) as T
        }
    }
}
