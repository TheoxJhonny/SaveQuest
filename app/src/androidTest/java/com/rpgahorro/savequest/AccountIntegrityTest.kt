package com.rpgahorro.savequest

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.*
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class AccountIntegrityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun migrationFromExportedV4PreservesMovementsContributionsAndRewards() = runBlocking {
        val name = "accounts-test-${UUID.randomUUID()}.db"
        val fixture = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.rpgahorro.savequest.data.local.SaveQuestDatabase/4.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val entities = fixture.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = fixture.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO transactions VALUES (7, 'INCOME', 60000, 'Pago', 'Original', 123, NULL, -33.0, -70.0)")
            db.execSQL("INSERT INTO transactions VALUES (8, 'EXPENSE', 10000, 'Compra', 'Original', 124, NULL, NULL, NULL)")
            db.execSQL("INSERT INTO saving_goals VALUES (4, 'Viaje', 100000, 122)")
            db.execSQL("INSERT INTO goal_contributions VALUES (9, 4, 10000, 125)")
            db.execSQL("INSERT INTO user_progress VALUES (1, 130)")
            db.execSQL("INSERT INTO challenges VALUES ('track_3_transactions', 'Mantén el ritmo', 'Registra 3 movimientos', 3, 3, 100, 1, 1)")
            db.version = 4
        }
        val db = Room.databaseBuilder(context, SaveQuestDatabase::class.java, name)
            .addMigrations(MIGRATION_4_5, com.rpgahorro.savequest.data.local.MIGRATION_5_6).addCallback(DatabaseDefaults).build()
        try {
            val transactions = db.transactionDao().getAll().first()
            assertEquals(setOf(7L, 8L), transactions.map { it.id }.toSet())
            assertTrue(transactions.all { it.accountId == 1L && it.description == "Original" })
            val original = transactions.first { it.id == 7L }
            assertEquals(-33.0, original.latitude!!, 0.0)
            assertEquals(-70.0, original.longitude!!, 0.0)
            assertEquals(123L, original.date)
            val contribution = db.savingGoalDao().observeGoals().first().single().contributions.single()
            assertEquals(9L, contribution.id)
            assertEquals(1L, contribution.accountId)
            assertEquals(10000L, contribution.amount)
            assertEquals(125L, contribution.date)
            val account = db.accountDao().observeBalances().first().single()
            assertEquals(50000L, account.balance)
            assertEquals(10000L, account.reserved)
            assertEquals(40000L, account.available)
            assertEquals(130, db.userProgressDao().observeProgress().first()!!.totalXp)
            assertTrue(db.challengeDao().getById("track_3_transactions")!!.rewardClaimed)
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun reservationsCannotUseMoneyFromAnotherAccountAndNamesAreUnique() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, SaveQuestDatabase::class.java).addCallback(DatabaseDefaults).build()
        try {
            val cash = db.accountDao().create("Efectivo")
            val repository = TransactionRepository(db)
            repository.record(TransactionType.INCOME, 10000, "Pago", "", accountId = 1)
            repository.record(TransactionType.INCOME, 2000, "Pago", "", accountId = cash)
            val goal = db.savingGoalDao().createGoal("Viaje", 20000)
            try { db.savingGoalDao().contribute(goal, 3000, cash); fail("No debe usar otra cuenta") }
            catch (_: IllegalArgumentException) { }
            db.savingGoalDao().contribute(goal, 8000, 1)
            db.savingGoalDao().contribute(goal, 1000, cash)
            val accounts = db.accountDao().observeBalances().first()
            assertEquals(12000L, accounts.sumOf { it.balance })
            assertEquals(9000L, accounts.sumOf { it.reserved })
            assertEquals(2000L, accounts.first { it.account.id == 1L }.available)
            assertEquals(1000L, accounts.first { it.account.id == cash }.available)
            assertEquals(2, db.transactionDao().getAll().first().size)
            try { db.accountDao().create("  efectivo  "); fail("Nombre duplicado") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
            try { repository.record(TransactionType.EXPENSE, 100, "Compra", "", 999999); fail("Cuenta inexistente") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertEquals(2, db.transactionDao().getAll().first().size)
            assertEquals(20, db.userProgressDao().observeProgress().first()!!.totalXp)
        } finally { db.close() }
    }
}
