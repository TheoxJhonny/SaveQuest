package com.rpgahorro.savequest

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class DeviceCredentialTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun cancelKeepsLockedAndSystemPinUnlocksThenBackgroundRelocks() {
        // Solo ejecutar con un PIN ficticio en un emulador desechable, nunca en un teléfono personal.
        val pin = InstrumentationRegistry.getArguments().getString("testDevicePin")
        assumeTrue(pin != null && pin.matches(Regex("[0-9]{6}")))
        compose.onNodeWithText("Entrar con PIN del teléfono").assertIsEnabled().performClick()
        waitForSystemDialog()
        shell("input keyevent 4")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Acceso cancelado. Tus registros siguen bloqueados.").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithText("PATRIMONIO TOTAL").assertDoesNotExist()
        compose.onNodeWithText("Entrar con PIN del teléfono").performClick()
        // Esperar a que Android exponga el campo de su diálogo, no sustituir su verificación.
        waitForSystemDialog()
        shell("input text $pin")
        shell("input keyevent 66")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("PATRIMONIO TOTAL").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithText("PATRIMONIO TOTAL").assertExists()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithText("Tu aventura, protegida").assertIsDisplayed()
        compose.onNodeWithText("PATRIMONIO TOTAL").assertDoesNotExist()
    }

    private fun waitForSystemDialog() {
        compose.waitUntil(15_000) {
            val root = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow
            root != null && root.packageName?.toString() != "com.rpgahorro.savequest" && containsCredentialField(root)
        }
    }

    private fun containsCredentialField(node: android.view.accessibility.AccessibilityNodeInfo): Boolean {
        if (node.isPassword || node.text?.toString() == "Desbloquear SaveQuest") return true
        return (0 until node.childCount).any { index -> node.getChild(index)?.let(::containsCredentialField) == true }
    }

    private fun shell(command: String) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }
}
