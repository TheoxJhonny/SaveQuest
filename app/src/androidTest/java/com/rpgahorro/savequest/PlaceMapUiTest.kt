package com.rpgahorro.savequest

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.entity.*
import com.rpgahorro.savequest.data.model.*
import com.rpgahorro.savequest.data.repository.PlaceCatalog
import com.rpgahorro.savequest.ui.map.*
import com.rpgahorro.savequest.ui.theme.SaveQuestTheme
import com.rpgahorro.savequest.ui.transactions.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PlaceMapUiTest {
    @get:Rule val compose = createComposeRule()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun realBusinessMarkerAndLabelRenderOfflineAndCanBeSelected() = runBlocking {
        val place =
            PlaceCatalog(context.assets).load().first { it.category == PlaceCategory.GROCERIES }
        val loaded = AtomicBoolean(false)
        val selected = AtomicReference<List<String>>(emptyList())
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(Manifest.permission.INTERNET),
        )
        compose.setContent {
            SaveQuestTheme {
                OfflineMap(
                    emptyList(),
                    Modifier.fillMaxSize().testTag("business-map"),
                    places = listOf(place),
                    focusedPlace = place,
                    onBusiness = { selected.set(it) },
                    onReady = { loaded.set(true) },
                )
            }
        }
        compose.waitUntil(60000) { loaded.get() }
        // El mapa nativo anima la cámara con reloj real, no con el reloj de Compose.
        Thread.sleep(1500)
        val deadline = System.currentTimeMillis() + 15000
        while (selected.get().isEmpty() && System.currentTimeMillis() < deadline) {
            nativeTouch("business-map", 80)
            Thread.sleep(500)
        }
        saveScreenshot("savequest-comercio-mapa.png")
        assertEquals(listOf(place.id), selected.get())
    }

    @Test
    fun searchOpensRealExpenseRouteAndSavesMerchantSnapshot() = runBlocking {
        val app = context.applicationContext as SaveQuestApplication
        val catalog = app.placeCatalog.load()
        val place =
            catalog
                .groupBy { it.name }
                .values
                .first { it.size == 1 && it.first().name.length in 6..25 }
                .single()
        compose.setContent { SaveQuestTheme { Surface { SaveQuestApp() } } }
        compose.onNodeWithText("Mapa").performClick()
        compose.onNodeWithText("Buscar comercio o dirección").performTextInput(place.name)
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("place-result-${place.id}").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("place-result-${place.id}").performClick()
        compose.waitUntil(60000) {
            compose.onAllNodesWithText("Abriendo Santiago…").fetchSemanticsNodes().isEmpty()
        }
        Thread.sleep(700)
        saveScreenshot("savequest-comercios-pantalla.png")
        compose.onNodeWithText("Registrar gasto aquí").performClick()
        compose.onNodeWithText("Registrar gasto").assertExists()
        compose.onNodeWithText(place.name).assertExists()
        compose.onNodeWithText(place.category.expenseCategory).assertExists()
        compose.onNodeWithText("Monto en pesos").performTextInput("3750")
        compose.onNodeWithText("Guardar movimiento").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose
                .onAllNodesWithText("Buscar comercio o dirección")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val row = app.database.transactionDao().getAll().first().first { it.placeId == place.id }
        assertEquals(place.name, row.placeName)
        assertEquals(3750L, row.amount)
        assertEquals(place.latitude, row.latitude!!, 0.0)
    }

    @Test
    fun draftRestoresBusinessAndRemovingLocationClearsTheLink() {
        val place =
            MapPlace("local:test", "Mi almacén", PlaceCategory.GROCERIES, "", -33.45, -70.66)
        val restore = StateRestorationTester(compose)
        var savedPlace: MapPlace? = place
        var called = false
        restore.setContent {
            SaveQuestTheme {
                Surface {
                    AddTransactionScreen(
                        TransactionType.EXPENSE,
                        {},
                        { _, _, _, _, lat, lon, merchant ->
                            savedPlace = merchant
                            called = true
                            assertNull(lat)
                            assertNull(lon)
                        },
                        listOf(AccountBalance(AccountEntity(1, "Principal", 0), 10000, 0)),
                        initialPlace = place,
                    )
                }
            }
        }
        compose.onNodeWithText("Monto en pesos").performTextInput("1000")
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Mi almacén").assertExists()
        compose.onNodeWithText("Quitar ubicación").performScrollTo().performClick()
        compose.onNodeWithText("Guardar movimiento").performScrollTo().performClick()
        assertTrue(called)
        assertNull(savedPlace)
    }

    @Test
    fun missingShopCanBeAddedFromLongPress() {
        var selected: MapPlace? = null
        compose.setContent {
            SaveQuestTheme { Surface { MapScreen(onRegisterExpense = { selected = it }) } }
        }
        compose.waitUntil(60000) {
            compose.onAllNodesWithText("Abriendo Santiago…").fetchSemanticsNodes().isEmpty()
        }
        nativeTouch("santiago-map", 1000)
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("Nombre del lugar").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Nombre del lugar").performTextInput("Almacén de prueba")
        compose.onNodeWithText("Usar lugar").performClick()
        compose.onNodeWithText("Registrar gasto aquí").performClick()
        assertEquals("Almacén de prueba", selected?.name)
        assertTrue(selected!!.id.startsWith("local:"))
        assertTrue(selected!!.latitude in -33.65..-33.25)
        assertTrue(selected!!.longitude in -70.85..-70.45)
    }

    private fun nativeTouch(tag: String, holdMillis: Long) {
        val point = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow.center
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = android.os.SystemClock.uptimeMillis()
        fun inject(action: Int) {
            val event =
                android.view.MotionEvent.obtain(
                    downTime,
                    android.os.SystemClock.uptimeMillis(),
                    action,
                    point.x,
                    point.y,
                    0,
                )
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            try {
                assertTrue(automation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
        inject(android.view.MotionEvent.ACTION_DOWN)
        Thread.sleep(holdMillis)
        inject(android.view.MotionEvent.ACTION_UP)
        compose.waitForIdle()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        val resolver = context.contentResolver
        val values =
            android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SaveQuest")
            }
        val uri =
            resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        resolver.openOutputStream(uri)!!.use {
            // Native MapView is a separate rendering surface.
            InstrumentationRegistry.getInstrumentation()
                .uiAutomation
                .takeScreenshot()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
