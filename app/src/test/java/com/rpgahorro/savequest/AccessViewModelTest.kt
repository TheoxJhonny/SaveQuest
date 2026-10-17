package com.rpgahorro.savequest

import com.rpgahorro.savequest.ui.security.AccessViewModel
import org.junit.Assert.*
import org.junit.Test

class AccessViewModelTest {
    @Test fun newSessionIsLockedAndOnlyCurrentAttemptCanUnlock() {
        val session = AccessViewModel()
        assertFalse(session.unlocked)
        session.authenticated(99)
        assertFalse(session.unlocked)
        val attempt = session.begin()!!
        assertNull(session.begin())
        session.authenticated(attempt)
        assertTrue(session.unlocked)
    }

    @Test fun backgroundingInvalidatesPendingSuccess() {
        val session = AccessViewModel()
        val old = session.begin()!!
        session.lock()
        session.authenticated(old)
        assertFalse(session.unlocked)
        val next = session.begin()!!
        session.failed(old, "callback atrasado")
        assertEquals(next, session.activeAttempt)
        session.authenticated(next)
        assertTrue(session.unlocked)
        session.lock()
        assertFalse(session.unlocked)
    }

    @Test fun cancellingDoesNotUnlockAndProcessRestartDoesNotRememberAccess() {
        val session = AccessViewModel()
        val attempt = session.begin()!!
        session.failed(attempt, "Cancelado")
        assertFalse(session.unlocked)
        assertEquals("Cancelado", session.message)
        session.authenticated(attempt)
        assertFalse(session.unlocked)
        session.authenticated(session.begin()!!)
        assertTrue(session.unlocked)
        assertFalse(AccessViewModel().unlocked)
    }
}
