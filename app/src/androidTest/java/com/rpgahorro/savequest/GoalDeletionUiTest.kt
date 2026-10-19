package com.rpgahorro.savequest

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.*
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.*
import com.rpgahorro.savequest.data.repository.*
import com.rpgahorro.savequest.ui.goals.*
import com.rpgahorro.savequest.ui.theme.SaveQuestTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GoalDeletionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun deletionRequiresConfirmationAndCancellationKeepsGoal() = runBlocking {
        val db =
            Room.inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    SaveQuestDatabase::class.java,
                )
                .addCallback(DatabaseDefaults)
                .build()
        val store = ViewModelStore()
        val owner =
            object : ViewModelStoreOwner {
                override val viewModelStore = store
            }
        try {
            db.savingGoalDao().createGoal("Notebook", 10_000)
            val vm =
                ViewModelProvider(
                    owner,
                    GoalsViewModel.Factory(
                        SavingGoalRepository(db.savingGoalDao()),
                        AccountRepository(db.accountDao()),
                    ),
                )[GoalsViewModel::class.java]
            compose.setContent { SaveQuestTheme { Surface { GoalsScreen(vm, 0) } } }
            compose.waitUntil(5000) {
                compose
                    .onAllNodesWithContentDescription("Eliminar meta Notebook")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            compose.onNodeWithContentDescription("Eliminar meta Notebook").performClick()
            compose.onNodeWithText("¿Eliminar Notebook?").assertIsDisplayed()
            compose.onNodeWithText("Cancelar").performClick()
            assertEquals(1, db.savingGoalDao().observeGoals().first().size)
            compose.onNodeWithContentDescription("Eliminar meta Notebook").performClick()
            compose.onNodeWithText("Eliminar meta").performClick()
            compose.waitUntil(5000) {
                compose
                    .onAllNodesWithText("Tu próxima aventura comienza con una meta.")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            assertTrue(db.savingGoalDao().observeGoals().first().isEmpty())
        } finally {
            compose.runOnIdle { store.clear() }
            db.close()
        }
    }
}
