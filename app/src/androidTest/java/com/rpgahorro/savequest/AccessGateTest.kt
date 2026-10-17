package com.rpgahorro.savequest

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class AccessGateTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launchAndRecreationDoNotExposeFinances() {
        compose.onNodeWithText("Tu aventura, protegida").assertIsDisplayed()
        compose.onNodeWithText("PATRIMONIO TOTAL").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Tu aventura, protegida").assertIsDisplayed()
        compose.onNodeWithText("PATRIMONIO TOTAL").assertDoesNotExist()
    }
}
