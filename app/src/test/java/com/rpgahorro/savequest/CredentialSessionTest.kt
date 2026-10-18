package com.rpgahorro.savequest

import com.rpgahorro.savequest.ui.security.AccessViewModel
import org.junit.Assert.*
import org.junit.Test

class CredentialSessionTest {
    @Test fun openingSystemActivityKeepsLockedAndSuccessWaitsForReturn() {
        val state = AccessViewModel()
        val id = state.begin(deviceCredential = true)!!
        state.backgrounded()
        assertFalse(state.unlocked)
        assertEquals(id, state.activeAttempt)
        state.resumed()
        assertFalse(state.unlocked)
        state.authenticated(id)
        assertFalse(state.unlocked)
        state.resumed()
        assertTrue(state.unlocked)
        state.backgrounded()
        assertFalse(state.unlocked)
    }

    @Test fun cancelledCredentialRejectsLateSuccess() {
        val state = AccessViewModel()
        val id = state.begin(true)!!
        state.failed(id, "Cancelado")
        state.authenticated(id)
        state.resumed()
        assertFalse(state.unlocked)
        assertFalse(state.credentialPending)
    }

    @Test fun staleCredentialCannotUnlockNewAttemptOrNewProcess() {
        val state = AccessViewModel()
        val old = state.begin(true)!!
        state.lock()
        state.begin(true)
        state.authenticated(old)
        state.resumed()
        assertFalse(state.unlocked)
        val restarted = AccessViewModel()
        restarted.authenticated(old)
        restarted.resumed()
        assertFalse(restarted.unlocked)
    }
}
