package com.rpgahorro.savequest

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.*
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GoalDeletionTest {
    private fun database() =
        Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                SaveQuestDatabase::class.java,
            )
            .addCallback(DatabaseDefaults)
            .build()

    private suspend fun withDatabase(block: suspend (SaveQuestDatabase) -> Unit) {
        val db = database()
        try {
            block(db)
        } finally {
            db.close()
        }
    }

    private suspend fun fund(db: SaveQuestDatabase, account: Long = 1) {
        db.transactionDao()
            .insert(
                TransactionEntity(
                    type = "INCOME",
                    amount = 10_000,
                    category = "Pago",
                    description = "",
                    date = 1,
                    accountId = account,
                )
            )
    }

    @Test
    fun deletingReleasesOnlyItsReservesAcrossAccountsWithoutChangingMoneyOrXp() = runBlocking {
        withDatabase { db ->
            val otherAccount = db.accountDao().create("Banco")
            fund(db)
            fund(db, otherAccount)
            val dao = db.savingGoalDao()
            val deleted = dao.createGoal("Notebook", 10_000)
            val kept = dao.createGoal("Viaje", 10_000)
            dao.contribute(deleted, 3000, 1)
            dao.contribute(deleted, 2000, otherAccount)
            dao.contribute(kept, 1000, 1)
            val transactions = db.transactionDao().getAll().first()
            val before = db.accountDao().observeBalances().first()
            val xp =
                db.openHelper.readableDatabase.query("SELECT totalXp FROM user_progress").use {
                    it.moveToFirst()
                    it.getLong(0)
                }
            dao.deleteGoal(deleted)
            val after = db.accountDao().observeBalances().first()
            assertEquals(before.map { it.balance }, after.map { it.balance })
            assertEquals(listOf(1000L, 0L), after.map { it.reserved })
            assertEquals(listOf(kept), dao.observeGoals().first().map { it.goal.id })
            assertEquals(transactions, db.transactionDao().getAll().first())
            db.openHelper.readableDatabase.query("SELECT totalXp FROM user_progress").use {
                it.moveToFirst()
                assertEquals(xp, it.getLong(0))
            }
            try {
                dao.contribute(deleted, 1)
                fail("Se aportó a una meta eliminada")
            } catch (_: IllegalArgumentException) {}
            val empty = dao.createGoal("Vacía", 100)
            dao.deleteGoal(empty)
        }
    }

    @Test
    fun failureRollsBackBothGoalAndContributions() = runBlocking {
        withDatabase { db ->
            fund(db)
            val dao = db.savingGoalDao()
            val id = dao.createGoal("Preservar", 10_000)
            dao.contribute(id, 4000)
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_goal_delete BEFORE DELETE ON saving_goals BEGIN SELECT RAISE(ABORT, 'test'); END"
            )
            try {
                dao.deleteGoal(id)
                fail("Se ignoró el fallo")
            } catch (_: android.database.sqlite.SQLiteException) {}
            assertEquals(4000L, dao.observeGoals().first().single().savedAmount)
        }
    }

    @Test
    fun simultaneousContributionAndDeletionLeaveNoOrphanReserves() = runBlocking {
        withDatabase { db ->
            fund(db)
            val dao = db.savingGoalDao()
            val id = dao.createGoal("Concurrente", 10_000)
            val contribution = async {
                try {
                    dao.contribute(id, 2000)
                } catch (_: IllegalArgumentException) {}
            }
            val deletion = async { dao.deleteGoal(id) }
            contribution.await()
            deletion.await()
            assertTrue(dao.observeGoals().first().isEmpty())
            assertEquals(0L, db.accountDao().observeBalances().first().single().reserved)
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use {
                assertFalse(it.moveToFirst())
            }
        }
    }
}
