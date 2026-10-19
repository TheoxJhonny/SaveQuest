package com.rpgahorro.savequest

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.SystemClock
import android.provider.ContactsContract
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.*
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

class PhoneContactPickerTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun systemPickerPrefillsAfterRelockAndOnlySavesOnConfirmation() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val automation = instrumentation.uiAutomation
        val resolver = context.contentResolver
        // Crear únicamente un contacto ficticio y quitar los privilegios ANTES del selector.
        automation.adoptShellPermissionIdentity(
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CONTACTS,
        )
        val rawId: Long
        try {
            val operations =
                arrayListOf(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build(),
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                            "SaveQuest Contacto Prueba",
                        )
                        .build(),
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, "+56912340000")
                        .withValue(
                            ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                        )
                        .build(),
                )
            rawId =
                ContentUris.parseId(
                    resolver.applyBatch(ContactsContract.AUTHORITY, operations).first().uri!!
                )
        } finally {
            automation.dropShellPermissionIdentity()
        }
        val db =
            Room.inMemoryDatabaseBuilder(context, SaveQuestDatabase::class.java)
                .addCallback(DatabaseDefaults)
                .build()
        val store = ViewModelStore()
        val owner =
            object : ViewModelStoreOwner {
                override val viewModelStore = store
            }
        var unlocked by mutableStateOf(true)
        var state: PhoneContactPickerState? = null
        try {
            assertEquals(
                PackageManager.PERMISSION_DENIED,
                context.checkSelfPermission(Manifest.permission.READ_CONTACTS),
            )
            val vm =
                ViewModelProvider(
                    owner,
                    ContactsViewModel.Factory(ContactRepository(db.contactDao())),
                )[ContactsViewModel::class.java]
            compose.setContent {
                val picker = rememberPhoneContactPicker()
                SideEffect { state = picker }
                SaveQuestTheme {
                    Surface {
                        if (unlocked)
                            ContactsScreen(
                                vm,
                                {},
                                picker.copy(
                                    launch = {
                                        unlocked = false
                                        picker.launch()
                                    }
                                ),
                            )
                        else Text("Bloqueado para prueba")
                    }
                }
            }
            compose.onNodeWithText("Elegir del teléfono").performClick()
            val deadline = SystemClock.uptimeMillis() + 15_000
            var chosen = false
            while (!chosen && SystemClock.uptimeMillis() < deadline) {
                val root = automation.rootInActiveWindow
                val node =
                    root
                        ?.findAccessibilityNodeInfosByText("SaveQuest Contacto Prueba")
                        ?.firstOrNull()
                if (node != null) {
                    var clickable: AccessibilityNodeInfo? = node
                    while (clickable != null && !clickable.isClickable) clickable = clickable.parent
                    chosen = clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
                }
                Thread.sleep(250)
            }
            fun texts(node: AccessibilityNodeInfo?): String {
                if (node == null) return "sin ventana"
                return "${node.packageName}: ${node.text} ${node.contentDescription} " +
                    (0 until node.childCount).joinToString { texts(node.getChild(it)) }
            }
            assertTrue("Selector: ${state?.error}; ${texts(automation.rootInActiveWindow)}", chosen)
            compose.waitUntil(10_000) { state?.draft != null || state?.error != null }
            assertNull(state?.error)
            compose.onNodeWithText("Bloqueado para prueba").assertIsDisplayed()
            assertTrue(db.contactDao().observeAll().first().isEmpty())
            compose.runOnIdle { unlocked = true }
            compose.onNodeWithText("SaveQuest Contacto Prueba").assertExists()
            compose.onNodeWithText("+56912340000").assertExists()
            assertTrue(db.contactDao().observeAll().first().isEmpty())
            compose.onNodeWithText("Guardar").performClick()
            compose.waitUntil(5000) {
                compose.onAllNodesWithText("Editar").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(
                "SaveQuest Contacto Prueba",
                db.contactDao().observeAll().first().single().name,
            )
        } finally {
            compose.runOnIdle { store.clear() }
            db.close()
            automation.adoptShellPermissionIdentity(Manifest.permission.WRITE_CONTACTS)
            try {
                resolver.delete(
                    ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawId),
                    null,
                    null,
                )
            } finally {
                automation.dropShellPermissionIdentity()
            }
        }
    }
}
