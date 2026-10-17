package com.rpgahorro.savequest

import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.ui.theme.SaveQuestTheme
import com.rpgahorro.savequest.ui.transactions.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MovementFormTest {
    private val accounts = listOf(com.rpgahorro.savequest.data.local.entity.AccountBalance(com.rpgahorro.savequest.data.local.entity.AccountEntity(1, "Principal"), 0, 0))
    @get:Rule val compose = createComposeRule()

    @Test fun formShowsErrorsAndPreservesInputAcrossRecreation() {
        val restoration = StateRestorationTester(compose)
        var calls = 0
        restoration.setContent {
            SaveQuestTheme { Surface {
                AddTransactionScreen(TransactionType.EXPENSE, {}, { _, _, _, _, _, _ -> calls++ }, accounts = accounts)
            } }
        }
        compose.onNodeWithText("Guardar movimiento").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa un monto válido mayor que cero.").assertExists()
        assertEquals(0, calls)
        compose.onNodeWithText("Monto en pesos").performScrollTo().performTextInput("5000")
        compose.onNodeWithText("Categoría").performScrollTo().performTextInput("Comida")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("5000").assertExists()
        compose.onNodeWithText("Comida").assertExists()
        compose.onNodeWithText("Guardar movimiento").performScrollTo().performClick()
        assertEquals(1, calls)
    }

    @Test fun savingDisablesSubmissionAndNavigationWaitsForSuccess() {
        var state by mutableStateOf(TransactionSaveState())
        var calls = 0
        var success = 0
        compose.setContent {
            SaveQuestTheme { Surface {
                AddTransactionScreen(TransactionType.INCOME, {}, { _, _, _, _, _, _ -> calls++; state = TransactionSaveState(saving = true) },
                    accounts = accounts, saveState = state, onSaved = { success++ })
            } }
        }
        compose.onNodeWithText("Monto en pesos").performTextInput("1000")
        compose.onNodeWithText("Categoría").performTextInput("Pago")
        compose.onNodeWithText("Guardar movimiento").performScrollTo().performClick()
        compose.onNodeWithText("Guardando…").assertIsNotEnabled()
        assertEquals(1, calls)
        assertEquals(0, success)
        compose.runOnIdle { state = TransactionSaveState(saved = true) }
        compose.waitForIdle()
        assertEquals(1, success)
    }
}
