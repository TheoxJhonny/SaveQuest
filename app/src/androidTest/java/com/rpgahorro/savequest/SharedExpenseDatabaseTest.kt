package com.rpgahorro.savequest

import androidx.room.Room
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.*
import com.rpgahorro.savequest.data.repository.*
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.domain.shared.*
import com.rpgahorro.savequest.ui.home.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class SharedExpenseDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun database() = Room.inMemoryDatabaseBuilder(context, SaveQuestDatabase::class.java).addCallback(DatabaseDefaults).build()

    @Test fun cashDebtsNetWorthAndReservationsStayConsistent() = runBlocking {
        val db = database()
        val store = ViewModelStore()
        try {
            val contacts = ContactRepository(db.contactDao())
            val ana = contacts.save(null, "Ana", "", "")
            val jose = contacts.save(null, "José", "", "")
            val account = db.accountDao().create("Efectivo")
            val tx = TransactionRepository(db)
            tx.record(TransactionType.INCOME, 100000, "Sueldo", "", account)
            val shared = SharedExpenseRepository(db)
            shared.record("self", "Cena", listOf(SharedItem("Menú", 3, 10000)), listOf(0, ana, jose), 0, account)
            val first = shared.expenses.first().single()
            assertEquals(10000L, first.expense.ownExpense)
            assertEquals(20000L, first.receivable)
            assertEquals(70000L, db.accountDao().observeBalances().first().single { it.account.id == account }.balance)
            val goal = db.savingGoalDao().createGoal("Viaje", 100000)
            try { db.savingGoalDao().contribute(goal, 75000, account); fail("No se puede reservar dinero adelantado") }
            catch (_: IllegalArgumentException) { }
            db.savingGoalDao().contribute(goal, 5000, account)
            shared.record("contact", "Compras", listOf(SharedItem("Compra", 1, 30000)), listOf(0, ana, jose), ana, null,
                mapOf(0L to 10000L, ana to 15000L, jose to 5000L))
            val second = shared.expenses.first().first { it.expense.id == "contact" }
            assertEquals(0L, second.expense.cashOutflow)
            assertEquals(10000L, second.payable)
            assertEquals(65000L, db.accountDao().observeBalances().first().single { it.account.id == account }.available)
            assertEquals(1, tx.transactions.first().size) // No duplicamos el gasto como movimiento ordinario.
            val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
            val vm = ViewModelProvider(owner, HomeViewModelFactory(tx, UserProgressRepository(db.userProgressDao()),
                ChallengeRepository(db.challengeDao()), SavingGoalRepository(db.savingGoalDao()), shared))[HomeViewModel::class.java]
            val state = withTimeout(5000) { vm.uiState.first { it.totalExpenses == 20000L } }
            assertEquals(80000L, state.balance)
            assertEquals(65000L, state.available)
            assertEquals(20000L, state.receivable)
            assertEquals(10000L, state.payable)
            assertEquals(130, state.totalXp)
            contacts.archive(ana, true)
            assertEquals(2, shared.expenses.first().size)
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        } finally { withContext(Dispatchers.Main) { store.clear() }; db.close() }
    }

    @Test fun retryAndConcurrentSubmissionChargeAndRewardOnlyOnce() = runBlocking {
        val db = database()
        try {
            val ana = ContactRepository(db.contactDao()).save(null, "Ana", "", "")
            val repo = SharedExpenseRepository(db)
            coroutineScope { (1..4).map { async(Dispatchers.IO) {
                repo.record("same-request", "Cena", listOf(SharedItem("Menú", 2, 5000)), listOf(0, ana), 0, 1)
            } }.awaitAll() }
            try {
                repo.record("same-request", "Cena modificada", listOf(SharedItem("Menú", 2, 5000)), listOf(0, ana), 0, 1)
                fail("No debe aceptar datos diferentes para un registro confirmado")
            } catch (_: IllegalArgumentException) { }
            assertEquals(1, repo.expenses.first().size)
            assertEquals(-10000L, db.accountDao().observeBalances().first().single().balance)
            assertEquals(10, db.userProgressDao().observeProgress().first()!!.totalXp)
            assertEquals(1, db.challengeDao().getById("track_3_transactions")!!.progress)
        } finally { db.close() }
    }

    @Test fun failureRollsBackHeaderItemsBalanceAndReward() = runBlocking {
        val db = database()
        try {
            val ana = ContactRepository(db.contactDao()).save(null, "Ana", "", "")
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_shares BEFORE INSERT ON shared_shares BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            try {
                SharedExpenseRepository(db).record("rollback", "Cena", listOf(SharedItem("Menú", 2, 5000)), listOf(0, ana), 0, 1)
                fail("Debe fallar la transacción")
            } catch (_: android.database.sqlite.SQLiteException) { }
            assertTrue(db.sharedExpenseDao().observeAll().first().isEmpty())
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM shared_items").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
            assertEquals(0L, db.accountDao().observeBalances().first().single().balance)
            assertEquals(0, db.userProgressDao().observeProgress().first()!!.totalXp)
        } finally { db.close() }
    }

    @Test fun archivedContactInvalidAccountAndInvalidSplitCannotBeSaved() = runBlocking {
        val db = database()
        try {
            val contacts = ContactRepository(db.contactDao())
            val ana = contacts.save(null, "Ana", "", "")
            val repo = SharedExpenseRepository(db)
            val items = listOf(SharedItem("Menú", 2, 5000))
            contacts.archive(ana, true)
            try { repo.record("a", "Cena", items, listOf(0, ana), 0, 1); fail("Archivado") } catch (_: IllegalArgumentException) { }
            contacts.archive(ana, false)
            try { repo.record("b", "Cena", items, listOf(0, ana), 0, 999); fail("Cuenta") } catch (_: IllegalArgumentException) { }
            try { repo.record("c", "Cena", items, listOf(0, ana), ana, null, mapOf(0L to 1L, ana to 1L)); fail("Reparto") } catch (_: IllegalArgumentException) { }
            assertTrue(repo.expenses.first().isEmpty())
            assertEquals(0, db.userProgressDao().observeProgress().first()!!.totalXp)
        } finally { db.close() }
    }
}
