package com.rpgahorro.savequest

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.*
import com.rpgahorro.savequest.data.model.*
import com.rpgahorro.savequest.data.repository.*
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PlacePersistenceTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val place =
        MapPlace(
            "osm:node:123",
            "Supermercado de prueba",
            PlaceCategory.GROCERIES,
            "Calle 123",
            -33.4489,
            -70.6693,
        )

    @Test
    fun merchantSurvivesReopeningAndChargesOnlyOnce() = runBlocking {
        val name = "place-${UUID.randomUUID()}.db"
        var db =
            Room.databaseBuilder(context, SaveQuestDatabase::class.java, name)
                .addCallback(DatabaseDefaults)
                .build()
        try {
            TransactionRepository(db)
                .record(
                    TransactionType.EXPENSE,
                    5000,
                    "Alimentación",
                    "Compra",
                    1,
                    place.latitude,
                    place.longitude,
                    place,
                )
            db.close()
            db =
                Room.databaseBuilder(context, SaveQuestDatabase::class.java, name)
                    .addCallback(DatabaseDefaults)
                    .build()
            val row = db.transactionDao().getAll().first().single()
            assertEquals(place.id, row.placeId)
            assertEquals(place.name, row.placeName)
            assertEquals(place.address, row.placeAddress)
            assertEquals(place.category.key, row.placeCategory)
            assertEquals(place.latitude, row.latitude!!, 0.0)
            assertEquals(-5000L, db.accountDao().observeBalances().first().single().balance)
            assertEquals(10, db.userProgressDao().observeProgress().first()!!.totalXp)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun mismatchedLocationCannotAttachAnUnrelatedBusiness() = runBlocking {
        val db =
            Room.inMemoryDatabaseBuilder(context, SaveQuestDatabase::class.java)
                .addCallback(DatabaseDefaults)
                .build()
        try {
            val repo = TransactionRepository(db)
            try {
                repo.record(TransactionType.EXPENSE, 5000, "Comida", "", 1, -33.0, -70.0, place)
                fail("Coordenadas diferentes")
            } catch (_: IllegalArgumentException) {}
            try {
                repo.record(
                    TransactionType.INCOME,
                    5000,
                    "Pago",
                    "",
                    1,
                    place.latitude,
                    place.longitude,
                    place,
                )
                fail("Ingreso con comercio")
            } catch (_: IllegalArgumentException) {}
            assertTrue(repo.transactions.first().isEmpty())
            assertEquals(0, db.userProgressDao().observeProgress().first()!!.totalXp)
        } finally {
            db.close()
        }
    }

    @Test
    fun offlineCatalogHasUniqueIdsAndGlyphFilesForEveryName() = runBlocking {
        val places = PlaceCatalog(context.assets).load()
        assertTrue(places.size > 100)
        assertEquals(places.size, places.map { it.id }.distinct().size)
        assertTrue(places.all { it.latitude in -33.65..-33.25 && it.longitude in -70.85..-70.45 })
        assertEquals(PlaceCategory.entries.toSet(), places.map { it.category }.toSet())
        val ranges = places.flatMap { it.name.map { char -> char.code / 256 * 256 } }.distinct()
        ranges.forEach { start ->
            context.assets.open("map/fonts/Noto Sans Regular/$start-${start + 255}.pbf").use {
                assertTrue(it.read() >= 0)
            }
        }
        assertEquals(place, MapPlace.fromJson(place.toJson()))
    }
}
