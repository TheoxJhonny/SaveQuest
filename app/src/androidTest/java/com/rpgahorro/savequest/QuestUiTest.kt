package com.rpgahorro.savequest

import android.graphics.Bitmap
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.SaveQuestDatabase
import com.rpgahorro.savequest.data.local.entity.*
import com.rpgahorro.savequest.data.repository.SavingGoalRepository
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.goals.*
import com.rpgahorro.savequest.ui.home.*
import com.rpgahorro.savequest.ui.theme.SaveQuestTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class QuestUiTest {
    @get:Rule val compose = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test fun homeShowsFinancialSeparationAndWorkingGoalAction() {
        var selected: Long? = null
        val state = HomeUiState(balance = 767010, totalIncome = 795000, totalExpenses = 27990,
            reserved = 315000, available = 452010, level = 2, totalXp = 150, currentLevelXp = 50,
            goals = listOf(SavingGoalWithContributions(SavingGoalEntity(7, "Notebook", 900000),
                listOf(GoalContributionEntity(goalId = 7, amount = 315000)))),
            challenges = listOf(ChallengeEntity("track_3_transactions", "Mantén el ritmo", "Registra 3 movimientos", 3, 2, 100)))
        compose.setContent {
            SaveQuestTheme {
                Scaffold(bottomBar = { SaveQuestBottomBar("home", {}) }) { padding ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
                        HomeScreen(state, onContribute = { selected = it })
                    }
                }
            }
        }
        compose.onNodeWithText("$767.010").assertExists()
        compose.onNodeWithText("$452.010").assertExists()
        compose.onNodeWithText("$315.000").assertExists()
        saveScreenshot("savequest-home-final.png")
        compose.onNodeWithText("+  Aportar a meta").performScrollTo().performClick()
        assertEquals(7L, selected)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Mantén el ritmo"))
        compose.onNodeWithText("Mantén el ritmo").assertIsDisplayed()
        saveScreenshot("savequest-misiones-final.png")
    }

    @Test fun goalsCreateValidateAndReserveRealFunds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SaveQuestDatabase::class.java).addCallback(com.rpgahorro.savequest.data.local.DatabaseDefaults).build()
        try {
            db.transactionDao().insert(TransactionEntity(type = "INCOME", amount = 10000, category = "Prueba", description = "", date = 1))
            val vm = GoalsViewModel(SavingGoalRepository(db.savingGoalDao()), com.rpgahorro.savequest.data.repository.AccountRepository(db.accountDao()))
            compose.setContent { val goals = vm.goals.collectAsState().value; SaveQuestTheme { Surface { GoalsScreen(vm, available = 10000 - goals.sumOf { it.savedAmount }) } } }
            compose.onNodeWithText("+  Nueva meta").performClick()
            compose.onNodeWithText("Nombre de la meta").performTextInput("Viaje")
            compose.onNodeWithText("Objetivo en pesos").performTextInput("20000")
            compose.onNodeWithText("Guardar").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("Viaje").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("+  Aportar a meta").performScrollTo().performClick()
            compose.onNodeWithText("Aporte en pesos").performTextInput("11000")
            compose.onNodeWithText("Guardar").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("No tienes suficiente dinero disponible.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Aporte en pesos").performTextReplacement("5000")
            compose.onNodeWithText("Guardar").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("25%").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(5000L, db.savingGoalDao().observeGoals().first().single().savedAmount)
            assertEquals(1, db.transactionDao().getAll().first().size)
            saveScreenshot("savequest-metas-final.png")
        } finally { db.close() }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SaveQuest")
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        resolver.openOutputStream(uri)!!.use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
