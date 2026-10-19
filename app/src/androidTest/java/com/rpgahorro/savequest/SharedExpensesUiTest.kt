package com.rpgahorro.savequest

import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.*
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.*
import com.rpgahorro.savequest.data.repository.*
import com.rpgahorro.savequest.ui.shared.*
import com.rpgahorro.savequest.ui.theme.SaveQuestTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SharedExpensesUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun draftSurvivesRecreationAndSavesOneSharedExpense() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, SaveQuestDatabase::class.java).addCallback(DatabaseDefaults).build()
        val contacts = ContactRepository(db.contactDao())
        contacts.save(null, "Ana", "", "")
        val store = ViewModelStore()
        val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
        val vm = ViewModelProvider(owner, SharedExpensesViewModel.Factory(SharedExpenseRepository(db), contacts, AccountRepository(db.accountDao())))[SharedExpensesViewModel::class.java]
        val restoration = StateRestorationTester(compose)
        fun scroll(text: String) { compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text)) }
        try {
            restoration.setContent { SaveQuestTheme { Surface { SharedExpensesScreen(vm, {}, {}) } } }
            compose.waitUntil(5000) { compose.onAllNodesWithText("Nuevo gasto compartido").fetchSemanticsNodes().any { it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled).not() } }
            compose.onNodeWithText("Nuevo gasto compartido").performClick()
            compose.onNodeWithText("Título del gasto").performTextInput("Cena de prueba")
            compose.onNodeWithText("Producto 1").performTextInput("Menú")
            compose.onNodeWithText("Precio unitario").performTextInput("10000")
            restoration.emulateSavedInstanceStateRestore()
            compose.onNodeWithText("Cena de prueba").assertExists()
            compose.onNodeWithText("10000").assertExists()
            scroll("Ana")
            compose.onNode(isToggleable()).performClick()
            scroll("Cuenta principal · Disponible $0")
            compose.onNodeWithTag("pay-account-1").performClick().assertIsSelected()
            scroll("Guardar gasto compartido")
            saveScreenshot("savequest-reparto.png")
            compose.onNodeWithText("Guardar gasto compartido").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("Nuevo gasto compartido").fetchSemanticsNodes().isNotEmpty() && compose.onAllNodesWithText("Título del gasto").fetchSemanticsNodes().isEmpty() }
            val detail = db.sharedExpenseDao().observeAll().first().single()
            assertEquals("Cena de prueba", detail.expense.title)
            assertEquals(5000L, detail.expense.ownExpense)
            assertEquals(10000L, detail.expense.cashOutflow)
            assertEquals(2, detail.shares.size)
            saveScreenshot("savequest-compartidos.png")
        } finally { compose.runOnIdle { store.clear() }; db.close() }
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
