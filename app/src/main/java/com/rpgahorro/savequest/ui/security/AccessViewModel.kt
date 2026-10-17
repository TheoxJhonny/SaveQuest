package com.rpgahorro.savequest.ui.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** La sesión vive solo en memoria: ni SavedStateHandle ni preferencias almacenan acceso. */
class AccessViewModel : ViewModel() {
    var unlocked by mutableStateOf(false)
        private set
    var activeAttempt by mutableStateOf<Long?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    private var sequence = 0L

    fun begin(): Long? {
        if (unlocked || activeAttempt != null) return null
        message = null
        return (++sequence).also { activeAttempt = it }
    }

    fun authenticated(attempt: Long) {
        if (activeAttempt != attempt) return
        activeAttempt = null
        unlocked = true
        message = null
    }

    fun failed(attempt: Long, reason: String) {
        if (activeAttempt != attempt) return
        activeAttempt = null
        unlocked = false
        message = reason
    }

    fun lock() {
        activeAttempt = null
        unlocked = false
        message = null
    }
}
