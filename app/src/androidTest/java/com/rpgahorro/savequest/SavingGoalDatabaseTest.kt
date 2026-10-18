package com.rpgahorro.savequest

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.MIGRATION_3_4
import com.rpgahorro.savequest.data.local.SaveQuestDatabase
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SavingGoalDatabaseTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun migrationPreservesExistingDataAndValidatesSchema() = runBlocking {
        // Base independiente: nunca abrir ni borrar savequest.db en estas pruebas.
        val name = "migration-test-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            // Esquema real generado por Room para SaveQuest v3.
            old.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount INTEGER NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, date INTEGER NOT NULL, goalId INTEGER, latitude REAL, longitude REAL)")
            old.execSQL("CREATE TABLE user_progress (id INTEGER NOT NULL, totalXp INTEGER NOT NULL, PRIMARY KEY(id))")
            old.execSQL("CREATE TABLE challenges (id TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, target INTEGER NOT NULL, progress INTEGER NOT NULL, xpReward INTEGER NOT NULL, completed INTEGER NOT NULL, rewardClaimed INTEGER NOT NULL, PRIMARY KEY(id))")
            old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            old.execSQL("INSERT INTO room_master_table VALUES (42, 'f71d5563f03b621c7070b5efbb9ad5fd')")
            repeat(2) {
                old.execSQL("INSERT INTO transactions VALUES (?, 'INCOME', 50000, 'Pago', 'Duplicado existente', 123, NULL, -33.0, -70.0)", arrayOf(it + 1))
            }
            old.execSQL("INSERT INTO user_progress VALUES (1, 130)")
            old.execSQL("INSERT INTO challenges VALUES ('track_3_transactions', 'Mantén el ritmo', 'Registra 3 movimientos', 3, 3, 100, 1, 1)")
            old.version = 3
        }
        val db = Room.databaseBuilder(context, SaveQuestDatabase::class.java, name)
            .addMigrations(MIGRATION_3_4, com.rpgahorro.savequest.data.local.MIGRATION_4_5, com.rpgahorro.savequest.data.local.MIGRATION_5_6).build()
        try {
            val transactions = db.transactionDao().getAll().first()
            assertEquals(2, transactions.size)
            assertEquals(setOf(1L, 2L), transactions.map { it.id }.toSet())
            transactions.forEach {
                assertEquals(50000L, it.amount)
                assertEquals("INCOME", it.type)
                assertEquals("Pago", it.category)
                assertEquals("Duplicado existente", it.description)
                assertEquals(123L, it.date)
                assertNull(it.goalId)
                assertEquals(-33.0, it.latitude!!, 0.0)
                assertEquals(-70.0, it.longitude!!, 0.0)
            }
            db.openHelper.readableDatabase.query("SELECT totalXp FROM user_progress WHERE id = 1").use {
                assertTrue(it.moveToFirst())
                assertEquals(130, it.getInt(0))
            }
            db.openHelper.readableDatabase.query("SELECT progress, completed, rewardClaimed FROM challenges").use {
                assertTrue(it.moveToFirst())
                assertEquals(3, it.getInt(0))
                assertEquals(1, it.getInt(1))
                assertEquals(1, it.getInt(2))
            }
            assertTrue(db.savingGoalDao().observeGoals().first().isEmpty())
            val id = db.savingGoalDao().createGoal("Emergencias", 20000)
            db.savingGoalDao().contribute(id, 5000)
            assertEquals(5000L, db.savingGoalDao().observeGoals().first().single().savedAmount)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun contributionsReserveFundsWithoutCreatingExpensesAndRejectInvalidAmounts() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, SaveQuestDatabase::class.java).addCallback(com.rpgahorro.savequest.data.local.DatabaseDefaults).build()
        try {
            val dao = db.savingGoalDao()
            db.transactionDao().insert(TransactionEntity(type = "INCOME", amount = 10000, category = "Pago", description = "", date = 1))
            db.transactionDao().insert(TransactionEntity(type = "EXPENSE", amount = 1000, category = "Compra", description = "", date = 2))
            val goal = dao.createGoal("  Viaje  ", 20000)
            dao.contribute(goal, 5000)
            dao.contribute(goal, 4000)
            val result = dao.observeGoals().first().single()
            assertEquals("Viaje", result.goal.name)
            assertEquals(9000L, result.savedAmount)
            assertEquals(2, db.transactionDao().getAll().first().size)
            expectInvalid { dao.contribute(goal, 1) }
            expectInvalid { dao.contribute(goal, 0) }
            expectInvalid { dao.contribute(goal, -1) }
            expectInvalid { dao.contribute(Long.MAX_VALUE, 1) }
            expectInvalid { dao.createGoal(" ", 100) }
            expectInvalid { dao.createGoal("Inválida", 0) }
            val small = dao.createGoal("Pequeña", 10)
            expectInvalid { dao.contribute(small, 11) }

            // La FK protege también escrituras directas a SQLite.
            try {
                db.openHelper.writableDatabase.execSQL("INSERT INTO goal_contributions (goalId, amount, date, accountId) VALUES (999999, 1, 1, 1)")
                fail("Se aceptó un aporte sin meta")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            try {
                db.openHelper.writableDatabase.execSQL("DELETE FROM saving_goals WHERE id = ?", arrayOf(goal))
                fail("Se borró una meta con aportes")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        } finally {
            db.close()
        }
    }

    @Test
    fun concurrentContributionsCannotReserveTheSameAvailableMoney() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, SaveQuestDatabase::class.java).addCallback(com.rpgahorro.savequest.data.local.DatabaseDefaults).build()
        try {
            db.transactionDao().insert(TransactionEntity(type = "INCOME", amount = 10000, category = "Pago", description = "", date = 1))
            val dao = db.savingGoalDao()
            val first = dao.createGoal("Primera", 10000)
            val second = dao.createGoal("Segunda", 10000)
            val results = listOf(first, second).map { id ->
                async {
                    try { dao.contribute(id, 7000); true }
                    catch (_: IllegalArgumentException) { false }
                }
            }.awaitAll()
            assertEquals(1, results.count { it })
            assertEquals(7000L, dao.observeGoals().first().sumOf { it.savedAmount })
        } finally {
            db.close()
        }
    }

    private suspend fun expectInvalid(block: suspend () -> Unit) {
        try {
            block()
            fail("La operación inválida fue aceptada")
        } catch (_: IllegalArgumentException) { }
    }
}
