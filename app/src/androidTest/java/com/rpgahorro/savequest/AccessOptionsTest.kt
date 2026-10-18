package com.rpgahorro.savequest

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.rpgahorro.savequest.ui.security.AccessScreen
import com.rpgahorro.savequest.ui.theme.SaveQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccessOptionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pinAvailabilityAndBusyStateControlEntry() {
        var hasPin by mutableStateOf(false)
        var busy by mutableStateOf(false)
        var requests = 0
        var settings = 0
        compose.setContent {
            SaveQuestTheme {
                AccessScreen("Prueba", busy, false, false, {}, {}, {}, hasPin,
                    onPin = { requests++; busy = true }, onConfigurePin = { settings++ })
            }
        }
        compose.onNodeWithText("Entrar con PIN del teléfono").assertIsNotEnabled()
        compose.onNodeWithText("Configurar bloqueo del teléfono").performScrollTo().performClick()
        assertEquals(1, settings)
        compose.runOnIdle { hasPin = true }
        compose.onNodeWithText("Configurar bloqueo del teléfono").assertDoesNotExist()
        compose.onNodeWithText("Entrar con PIN del teléfono").performScrollTo().performClick()
        assertEquals(1, requests)
        compose.onNodeWithText("Entrar con PIN del teléfono").assertIsNotEnabled()
    }
}
