package com.rpgahorro.savequest

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.ui.components.SaveQuestBottomBar
import com.rpgahorro.savequest.ui.map.*
import com.rpgahorro.savequest.ui.theme.SaveQuestTheme
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OfflineMapTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun bundledMapLoadsAndMarkerIsSelectableWithoutInternetPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(
            PackageManager.PERMISSION_DENIED,
            context.checkSelfPermission(Manifest.permission.INTERNET),
        )
        val loaded = AtomicBoolean(false)
        val selected = AtomicLong(-1)
        compose.setContent {
            SaveQuestTheme {
                OfflineMap(
                    listOf(MapPoint(42, -33.4489, -70.6693)),
                    Modifier.fillMaxSize().testTag("map"),
                    onReady = { loaded.set(true) },
                    onPoint = { selected.set(it.first()) },
                )
            }
        }
        compose.waitUntil(60_000) { loaded.get() }
        // El estilo puede terminar antes del primer fotograma; el toque verifica renderizado +
        // consulta real.
        val deadline = System.currentTimeMillis() + 30_000
        while (selected.get() != 42L && System.currentTimeMillis() < deadline) {
            compose.onNodeWithTag("map").performTouchInput { click(center) }
            Thread.sleep(500)
        }
        assertEquals(42L, selected.get())
    }

    @Test
    fun mapScreenShowsMovementDetail() {
        compose.setContent {
            SaveQuestTheme {
                Scaffold(bottomBar = { SaveQuestBottomBar("map", {}) }) { padding ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
                        MapScreen(
                            listOf(
                                TransactionEntity(
                                    id = 42,
                                    type = "EXPENSE",
                                    amount = 4500,
                                    category = "Comida",
                                    description = "Almuerzo en Santiago",
                                    date = 1,
                                    latitude = -33.4489,
                                    longitude = -70.6693,
                                ),
                                TransactionEntity(
                                    id = 43,
                                    type = "INCOME",
                                    amount = 10000,
                                    category = "Pago",
                                    description = "Venta",
                                    date = 2,
                                    latitude = -33.4489,
                                    longitude = -70.6693,
                                ),
                            )
                        )
                    }
                }
            }
        }
        compose.waitUntil(60_000) {
            compose.onAllNodesWithText("Abriendo Santiago…").fetchSemanticsNodes().isEmpty()
        }
        val deadline = System.currentTimeMillis() + 30_000
        while (
            compose
                .onAllNodesWithText("Movimientos en este lugar")
                .fetchSemanticsNodes()
                .isEmpty() && System.currentTimeMillis() < deadline
        ) {
            compose.onNodeWithTag("santiago-map").performTouchInput { click(center) }
            Thread.sleep(500)
        }
        compose.onNodeWithText("Movimientos en este lugar").assertIsDisplayed()
        compose.onNodeWithText("Almuerzo en Santiago").performClick()
        compose.onNodeWithText("Movimientos en este lugar").assertDoesNotExist()
        compose.onNodeWithText("Almuerzo en Santiago").assertIsDisplayed()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Compose puede estar inactivo mientras la ventana nativa aún retira el oscurecimiento
        // del diálogo. Esperar el fotograma real conserva la comprobación de cartografía.
        var bitmap = instrumentation.uiAutomation.takeScreenshot()
        fun countRoadPixels(): Int {
            var count = 0
            for (y in 0 until bitmap.height step 2) for (x in 0 until bitmap.width step 2) {
                val color = bitmap.getPixel(x, y) and 0xFFFFFF
                if (color == 0x55516F || color == 0xA494DA) count++
            }
            return count
        }
        val renderDeadline = android.os.SystemClock.uptimeMillis() + 15_000
        var roadPixels = countRoadPixels()
        while (roadPixels <= 500 && android.os.SystemClock.uptimeMillis() < renderDeadline) {
            Thread.sleep(250)
            bitmap.recycle()
            bitmap = instrumentation.uiAutomation.takeScreenshot()
            roadPixels = countRoadPixels()
        }
        val resolver = instrumentation.targetContext.contentResolver
        val values =
            android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "savequest-santiago.png")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SaveQuest")
            }
        val uri =
            resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        resolver.openOutputStream(uri)!!.use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        assertTrue(
            "La cartografía debe estar renderizada, no solo el marcador ($roadPixels píxeles)",
            roadPixels > 500,
        )
    }
}
