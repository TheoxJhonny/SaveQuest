package com.rpgahorro.savequest.ui.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Solo memoria: una recreación del proceso siempre comienza bloqueada. */
class AccessViewModel : ViewModel() {
    var unlocked by mutableStateOf(false)
        private set
    var activeAttempt by mutableStateOf<Long?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var credentialPending by mutableStateOf(false)
        private set
    private var credentialConfirmed = false
    private var sequence = 0L

    fun begin(deviceCredential: Boolean = false): Long? {
        if (unlocked || activeAttempt != null) return null
        message = null
        credentialPending = deviceCredential
        credentialConfirmed = false
        return (++sequence).also { activeAttempt = it }
    }

    fun authenticated(attempt: Long) {
        if (activeAttempt != attempt) return
        if (credentialPending) {
            // El sistema puede responder antes de devolver la app al primer plano.
            credentialConfirmed = true
        } else unlock()
    }

    fun resumed() {
        if (credentialPending && credentialConfirmed && activeAttempt != null) unlock()
    }

    fun backgrounded() {
        // El diálogo de PIN pertenece a Android y puede cubrir completamente la actividad.
        // Esperarlo no abre la sesión; solo un resultado válido y el regreso pueden hacerlo.
        if (!credentialPending) lock()
    }

    private fun unlock() {
        activeAttempt = null
        credentialPending = false
        credentialConfirmed = false
        unlocked = true
        message = null
    }

    fun failed(attempt: Long, reason: String) {
        if (activeAttempt != attempt) return
        lock()
        message = reason
    }

    fun lock() {
        activeAttempt = null
        credentialPending = false
        credentialConfirmed = false
        unlocked = false
        message = null
    }
}
