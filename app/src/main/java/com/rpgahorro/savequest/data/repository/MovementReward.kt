package com.rpgahorro.savequest.data.repository

import com.rpgahorro.savequest.data.local.SaveQuestDatabase

/** Se llama únicamente dentro de la transacción que registra el gasto o ingreso. */
internal suspend fun awardMovement(database: SaveQuestDatabase) {
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
