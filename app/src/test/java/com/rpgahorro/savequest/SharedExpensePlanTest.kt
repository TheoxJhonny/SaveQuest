package com.rpgahorro.savequest

import com.rpgahorro.savequest.domain.shared.*
import org.junit.Assert.*
import org.junit.Test

class SharedExpensePlanTest {
    private fun item(amount: Long) = listOf(SharedItem("Compra", 1, amount))

    @Test fun payingForOthersSeparatesCashFromOwnExpense() {
        val plan = SharedExpensePlan.equal(item(30000), listOf(0, 7, 9), 0)
        assertEquals(30000L, plan.cashOutflow)
        assertEquals(10000L, plan.ownExpense)
        assertEquals(20000L, plan.debts.sumOf { it.receivable })
        assertEquals(0L, plan.debts.sumOf { it.payable })
        assertEquals(-plan.ownExpense, -plan.cashOutflow + plan.debts.sumOf { it.receivable - it.payable })
    }

    @Test fun anotherPayerCreatesOnlyMyDebtAndNoCashMovement() {
        val plan = SharedExpensePlan.equal(item(30000), listOf(0, 7, 9), 7)
        assertEquals(0L, plan.cashOutflow)
        assertEquals(10000L, plan.ownExpense)
        assertEquals(listOf(ContactDebt(7, payable = 10000)), plan.debts)
    }

    @Test fun remaindersAreExactAndIndependentOfSelectionOrder() {
        val first = SharedExpensePlan.equal(item(10000), listOf(9, 0, 7), 0)
        val second = SharedExpensePlan.equal(item(10000), listOf(7, 9, 0), 0)
        assertEquals(mapOf(0L to 3334L, 7L to 3333L, 9L to 3333L), first.shares)
        assertEquals(first.shares, second.shares)
        assertEquals(first.total, first.shares.values.sum())
    }

    @Test fun itemQuantitiesAndCustomSharesAreValidated() {
        val items = listOf(SharedItem("Entradas", 2, 5000), SharedItem("Traslado", 1, 3000))
        val plan = SharedExpensePlan.custom(items, listOf(0, 2), 0, mapOf(0L to 8000L, 2L to 5000L))
        assertEquals(13000L, plan.total)
        assertEquals(8000L, plan.ownExpense)
        assertEquals(5000L, plan.debts.single().receivable)
        invalid { SharedExpensePlan.custom(items, listOf(0, 2), 0, mapOf(0L to 8000L, 2L to 4000L)) }
        invalid { SharedExpensePlan.custom(items, listOf(0, 2), 0, mapOf(0L to 13000L)) }
        invalid { SharedExpensePlan.custom(items, listOf(0, 2), 0, mapOf(0L to -1L, 2L to 13001L)) }
    }

    @Test fun invalidParticipantsItemsAndOverflowsCannotProduceAPlan() {
        invalid { SharedExpensePlan.equal(item(100), listOf(0, 0, 2), 0) }
        invalid { SharedExpensePlan.equal(item(100), listOf(2, 3), 2) }
        invalid { SharedExpensePlan.equal(item(100), listOf(0, 2), 3) }
        invalid { SharedExpensePlan.equal(item(0), listOf(0, 2), 0) }
        invalid { SharedExpensePlan.equal(listOf(SharedItem("", 1, 10)), listOf(0, 2), 0) }
        invalid { SharedExpensePlan.equal(listOf(SharedItem("Grande", 2, Long.MAX_VALUE)), listOf(0, 2), 0) }
        invalid { SharedExpensePlan.equal(item(Long.MAX_VALUE) + item(1), listOf(0, 2), 0) }
        invalid { SharedExpensePlan.custom(item(100), listOf(0, 2), 0, mapOf(0L to Long.MAX_VALUE, 2L to Long.MAX_VALUE)) }
    }

    @Test fun smallAndMaximumTotalsConserveEveryPeso() {
        for (total in listOf(1L, 2L, 7L, 10001L, Long.MAX_VALUE)) {
            val plan = SharedExpensePlan.equal(item(total), listOf(0, 2, 3), 0)
            assertEquals(total, plan.shares.values.fold(0L, Math::addExact))
            assertTrue(plan.shares.values.max() - plan.shares.values.min() <= 1)
        }
    }

    private fun invalid(action: () -> Unit) {
        try { action(); fail("Debe rechazar la operación") } catch (_: IllegalArgumentException) { }
    }
}
