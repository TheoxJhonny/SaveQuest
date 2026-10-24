package com.rpgahorro.savequest

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.ui.map.*
import com.rpgahorro.savequest.ui.theme.SaveQuestTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class HeroLocationTest {
    @get:Rule val compose = createComposeRule()
    private fun fix(age: Long = 0) = Location(LocationManager.GPS_PROVIDER).apply {
        latitude = -33.4200; longitude = -70.6200; accuracy = 5f
        time = System.currentTimeMillis() - age
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos() - age * 1_000_000
    }

    @Test fun staleFixCannotCreateHeroOrSatisfyRecenter() {
        val state = HeroLocationState()
        state.pendingCenter = true
        state.accept(fix(120_000))
        assertNull(state.location)
        assertEquals(0, state.centerRequest)
        state.accept(fix())
        assertNotNull(state.location)
        assertEquals(1, state.centerRequest)
        val target = state.centerTarget!!.longitude
        state.accept(fix().apply { longitude = -70.61 })
        assertEquals("Live updates must not drag the camera", target, state.centerTarget!!.longitude, 0.0)
    }

    @Suppress("DEPRECATION")
    @Test fun gpsHeroRendersAndButtonReturnsAfterPanning() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fun shell(command: String): String = android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
        val previous = Regex("MOCK_LOCATION: (allow|ignore|deny|default|foreground)")
            .find(shell("appops get ${context.packageName} android:mock_location"))?.groupValues?.get(1) ?: "default"
        shell("appops set ${context.packageName} android:mock_location allow")
        var providerAdded = false
        try {
            manager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false, false, false, false, false, 1, 1)
            providerAdded = true
            manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, fix())
            val loaded = AtomicBoolean(false)
            compose.setContent {
                SaveQuestTheme { OfflineMap(emptyList(), Modifier.fillMaxSize().testTag("hero-map"), initialZoom = 16.5, onReady = { loaded.set(true) }) }
            }
            compose.waitUntil(60_000) { loaded.get() }
            manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, fix())
            compose.onNodeWithContentDescription("Volver a mi ubicación").performClick()
            fun heroNearCenter(): Boolean {
                val bitmap = instrumentation.uiAutomation.takeScreenshot()
                var gold = 0
                for (y in bitmap.height * 3 / 10 until bitmap.height * 6 / 10)
                    for (x in bitmap.width * 4 / 10 until bitmap.width * 6 / 10)
                        if (bitmap.getPixel(x,y) and 0xFFFFFF == 0xFFCA62) gold++
                bitmap.recycle()
                return gold > 10
            }
            compose.waitUntil(30_000) { heroNearCenter() }
            compose.onNodeWithTag("hero-map").performTouchInput { swipeLeft() }
            Thread.sleep(1000)
            assertFalse("Panning must move the hero off center", heroNearCenter())
            manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, fix())
            compose.onNodeWithContentDescription("Volver a mi ubicación").performClick()
            compose.waitUntil(20_000) { heroNearCenter() }
            // MediaStore survives test-package cleanup so the actual rendering can be reviewed.
            fun export(name: String, bitmap: Bitmap) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,name)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png")
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/SaveQuest")
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)!!
                context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                bitmap.recycle()
            }
            export("savequest-hero-map-final.png",instrumentation.uiAutomation.takeScreenshot())
            export("savequest-hero-icon-final.png",heroLocationIcon())
        } finally {
            if (providerAdded) manager.removeTestProvider(LocationManager.GPS_PROVIDER)
            shell("appops set ${context.packageName} android:mock_location $previous")
        }
    }
}
