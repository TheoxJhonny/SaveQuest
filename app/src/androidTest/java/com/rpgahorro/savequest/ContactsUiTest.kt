package com.rpgahorro.savequest

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.*
import com.rpgahorro.savequest.data.repository.ContactRepository
import com.rpgahorro.savequest.ui.contacts.*
import com.rpgahorro.savequest.ui.theme.SaveQuestTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ContactsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun contactsCanBeCreatedEditedArchivedAndRestored() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, SaveQuestDatabase::class.java)
            .addCallback(DatabaseDefaults).build()
        val store = ViewModelStore()
        val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
        val vm = ViewModelProvider(owner, ContactsViewModel.Factory(ContactRepository(db.contactDao())))[ContactsViewModel::class.java]
        val restoration = StateRestorationTester(compose)
        try {
            restoration.setContent { SaveQuestTheme { Surface { ContactsScreen(vm, {}) } } }
            compose.onNodeWithText("Nuevo contacto").performClick()
            compose.onNodeWithText("Guardar").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("El nombre debe tener entre 1 y 80 caracteres.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Nombre").performTextInput("Ana")
            compose.onNodeWithText("Teléfono (opcional)").performTextInput("+56912345678")
            restoration.emulateSavedInstanceStateRestore()
            compose.onNodeWithText("Ana").assertExists()
            compose.onNodeWithText("Guardar").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("Editar").fetchSemanticsNodes().isNotEmpty() }
            val original = db.contactDao().observeAll().first().single()
            compose.onNodeWithText("Editar").performClick()
            compose.onNodeWithText("Nombre").performTextReplacement("Ana Pérez")
            compose.onNodeWithText("Guardar").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("Guardar").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithText("Ana Pérez").assertExists()
            compose.onNodeWithText("Archivar").performClick()
            compose.onNodeWithText("Confirmar").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("Ana Pérez").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithText("Archivados").performClick()
            compose.onNodeWithText("Ana Pérez").assertExists()
            compose.onNodeWithText("Restaurar").performClick()
            compose.onNodeWithText("Confirmar").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("Ana Pérez").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithText("Activos").performClick()
            compose.onNodeWithText("Ana Pérez").assertExists()
            compose.onNodeWithText("Buscar por nombre o teléfono").performTextInput("nadie")
            compose.onNodeWithText("No se encontraron contactos.").assertExists()
            val final = db.contactDao().observeAll().first().single()
            assertEquals(original.id, final.id)
            assertFalse(final.archived)
        } finally { compose.runOnIdle { store.clear() }; db.close() }
    }
}
