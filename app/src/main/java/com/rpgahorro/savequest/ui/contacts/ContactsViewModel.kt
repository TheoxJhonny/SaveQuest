package com.rpgahorro.savequest.ui.contacts

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rpgahorro.savequest.data.local.entity.ContactEntity
import com.rpgahorro.savequest.data.repository.ContactRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ContactsViewModel(private val repository: ContactRepository) : ViewModel() {
    var loadError by mutableStateOf<String?>(null)
        private set
    val contacts = repository.contacts.catch {
        loadError = "No se pudieron cargar tus contactos. Vuelve a abrir esta pantalla."
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null as List<ContactEntity>?)
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var saved by mutableStateOf(false)
        private set

    fun clearStatus() {
        if (!saving) {
            error = null; saved = false
        }
    }

    fun save(id: Long?, name: String, phone: String, notes: String) =
        change { repository.save(id, name, phone, notes) }

    fun archive(id: Long, archived: Boolean) = change { repository.archive(id, archived) }

    private fun change(action: suspend () -> Unit) {
        if (saving || saved) return
        saving = true
        error = null
        viewModelScope.launch {
            try {
                action(); saved = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                error = e.message
            } catch (_: Exception) {
                error = "No se pudo guardar el contacto. Inténtalo nuevamente."
            } finally {
                saving = false
            }
        }
    }

    class Factory(private val repository: ContactRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ContactsViewModel::class.java))
            return ContactsViewModel(repository) as T
        }
    }
}
