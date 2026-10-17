package com.rpgahorro.savequest.data.repository

import androidx.room.withTransaction
import com.rpgahorro.savequest.data.local.SaveQuestDatabase
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.data.model.TransactionType

class TransactionRepository(private val database: SaveQuestDatabase) {
    val transactions = database.transactionDao().getAll()

    /** El movimiento y su recompensa se confirman juntos o se revierten juntos. */
    suspend fun record(type: TransactionType, amount: Long, category: String, description: String) {
        require(type == TransactionType.INCOME || type == TransactionType.EXPENSE) {
            "Los aportes se registran desde Metas."
        }
        require(amount > 0) { "El monto debe ser mayor que cero." }
        require(category.isNotBlank()) { "Escribe una categoría." }
        database.withTransaction {
            database.transactionDao().insert(TransactionEntity(
                type = type.name, amount = amount, category = category.trim(),
                description = description.trim(), date = System.currentTimeMillis()
            ))
            database.userProgressDao().addXp(10)
            val dao = database.challengeDao()
            val challenge = dao.getById("track_3_transactions")
            if (challenge != null && !challenge.completed) {
                val progress = (challenge.progress + 1).coerceAtMost(challenge.target)
                dao.updateProgress(challenge.id, progress)
                if (progress >= challenge.target) {
                    dao.completeChallenge(challenge.id)
                    if (!challenge.rewardClaimed) {
                        database.userProgressDao().addXp(challenge.xpReward)
                        dao.markRewardClaimed(challenge.id)
                    }
                }
            }
        }
    }
}
