package com.rpgahorro.savequest.data.repository

import androidx.room.withTransaction
import com.rpgahorro.savequest.data.local.SaveQuestDatabase
import com.rpgahorro.savequest.data.local.entity.*
import com.rpgahorro.savequest.domain.shared.*

class SharedExpenseRepository(private val database: SaveQuestDatabase) {
    val expenses = database.sharedExpenseDao().observeAll()

    /** El ID identifica un intento de registro; reintentarlo no duplica el cargo. */
    suspend fun record(id: String, title: String, items: List<SharedItem>, participants: List<Long>,
        payerId: Long, accountId: Long?, customShares: Map<Long, Long>? = null) {
        require(id.isNotBlank()) { "Falta el identificador del registro." }
        require(title.trim().length in 1..100) { "Escribe un título de hasta 100 caracteres." }
        val plan = if (customShares == null) SharedExpensePlan.equal(items, participants, payerId)
            else SharedExpensePlan.custom(items, participants, payerId, customShares)
        database.withTransaction {
            val dao = database.sharedExpenseDao()
            dao.find(id)?.let { existing ->
                require(existing.expense.title == title.trim() &&
                    existing.expense.payerContactId == payerId.takeUnless { it == SELF_PARTICIPANT } &&
                    existing.expense.accountId == accountId.takeIf { payerId == SELF_PARTICIPANT } &&
                    existing.shares.associate { it.participantId to it.amount } == plan.shares &&
                    existing.items.sortedBy { it.position }.map { SharedItem(it.name, it.quantity, it.unitPrice) } ==
                        items.map { it.copy(name = it.name.trim()) }) {
                    "Este registro ya se guardó con otros datos. Vuelve al historial para revisarlo."
                }
                return@withTransaction
            }
            val names = participants.associateWith { participant ->
                if (participant == SELF_PARTICIPANT) "Yo" else {
                    val contact = requireNotNull(database.contactDao().find(participant)) { "Un contacto ya no existe." }
                    require(!contact.archived) { "Restaura el contacto ${contact.name} antes de incluirlo." }
                    contact.name
                }
            }
            val payingAccount = if (payerId == SELF_PARTICIPANT) {
                requireNotNull(accountId) { "Selecciona la cuenta desde la que pagaste." }.also {
                    require(database.accountDao().exists(it)) { "La cuenta ya no existe." }
                }
            } else null
            dao.insert(SharedExpenseEntity(id, title.trim(), System.currentTimeMillis(),
                payerId.takeUnless { it == SELF_PARTICIPANT }, payingAccount, plan.total, plan.ownExpense, plan.cashOutflow))
            dao.insertItems(items.mapIndexed { index, item -> SharedItemEntity(id, index, item.name.trim(), item.quantity, item.unitPrice) })
            dao.insertShares(plan.shares.map { (participant, amount) ->
                SharedShareEntity(id, participant, participant.takeUnless { it == SELF_PARTICIPANT }, names.getValue(participant), amount)
            })
            awardMovement(database)
        }
    }
}
