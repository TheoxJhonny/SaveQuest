package com.rpgahorro.savequest

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.DatabaseDefaults
import com.rpgahorro.savequest.data.local.SaveQuestDatabase
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.data.repository.TransactionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MovementIntegrityTest {
    private fun database() = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext, SaveQuestDatabase::class.java
    ).addCallback(DatabaseDefaults).build()

    @Test fun freshInstallHasProgressAndChallengeAndOpeningAgainDoesNotResetThem() = runBlocking {
        val db = database()
        try {
            assertEquals(0, db.userProgressDao().observeProgress().first()!!.totalXp)
            assertEquals(1, db.challengeDao().observeChallenges().first().size)
            TransactionRepository(db).record(TransactionType.INCOME, 100, "Pago", "")
            DatabaseDefaults.onOpen(db.openHelper.writableDatabase)
            assertEquals(10, db.userProgressDao().observeProgress().first()!!.totalXp)
            assertEquals(1, db.challengeDao().getById("track_3_transactions")!!.progress)
        } finally { db.close() }
    }

    @Test fun concurrentMovementsGrantExactlyOneChallengeReward() = runBlocking {
        val db = database()
        try {
            val repository = TransactionRepository(db)
            (1..6).map { async { repository.record(TransactionType.INCOME, 100, "Pago", "") } }.awaitAll()
            assertEquals(6, db.transactionDao().getAll().first().size)
            assertEquals(160, db.userProgressDao().observeProgress().first()!!.totalXp)
            val challenge = db.challengeDao().getById("track_3_transactions")!!
            assertEquals(3, challenge.progress)
            assertTrue(challenge.completed && challenge.rewardClaimed)
        } finally { db.close() }
    }

    @Test fun failedRewardWriteRollsBackMovementAndXp() = runBlocking {
        val db = database()
        try {
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER fail_progress BEFORE UPDATE ON challenges
                BEGIN SELECT RAISE(ABORT, 'fallo simulado'); END
            """.trimIndent())
            try {
                TransactionRepository(db).record(TransactionType.EXPENSE, 100, "Compra", "")
                fail("La escritura debía fallar")
            } catch (_: android.database.sqlite.SQLiteException) { }
            assertTrue(db.transactionDao().getAll().first().isEmpty())
            assertEquals(0, db.userProgressDao().observeProgress().first()!!.totalXp)
            assertEquals(0, db.challengeDao().getById("track_3_transactions")!!.progress)
        } finally { db.close() }
    }
}
