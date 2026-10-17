package com.rpgahorro.savequest

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.*
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LocationPersistenceTest {
    @Test fun optionalCoordinatesPersistAndInvalidPairDoesNotGrantXp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SaveQuestDatabase::class.java).addCallback(DatabaseDefaults).build()
        try {
            val repository = TransactionRepository(db)
            repository.record(TransactionType.INCOME, 5000, "Pago", "", latitude = -33.45, longitude = -70.66)
            repository.record(TransactionType.EXPENSE, 1000, "Comida", "")
            try {
                repository.record(TransactionType.EXPENSE, 500, "Inválido", "", latitude = Double.NaN, longitude = -70.66)
                fail("Invalid coordinate was accepted")
            } catch (_: IllegalArgumentException) { }
            try {
                repository.record(TransactionType.EXPENSE, 500, "Incompleto", "", latitude = -33.45)
                fail("Incomplete coordinate pair was accepted")
            } catch (_: IllegalArgumentException) { }
            val rows = repository.transactions.first()
            assertEquals(2, rows.size)
            val income = rows.first { it.type == "INCOME" }
            assertEquals(-33.45, income.latitude!!, 0.000001)
            assertEquals(-70.66, income.longitude!!, 0.000001)
            assertNull(rows.first { it.type == "EXPENSE" }.latitude)
            val account = db.accountDao().observeBalances().first().single()
            assertEquals(4000L, account.balance)
            val cursor = db.openHelper.readableDatabase.query("SELECT totalXp FROM user_progress WHERE id = 1")
            cursor.use { assertTrue(it.moveToFirst()); assertEquals(20, it.getInt(0)) }
        } finally { db.close() }
    }
}
