package com.rpgahorro.savequest.data.repository

import com.rpgahorro.savequest.data.local.dao.ChallengeDao
import com.rpgahorro.savequest.data.local.entity.ChallengeEntity
import kotlinx.coroutines.flow.Flow

class ChallengeRepository(
    private val challengeDao: ChallengeDao
) {

    val challenges: Flow<List<ChallengeEntity>> =
        challengeDao.observeChallenges()

    suspend fun updateProgress(
        challengeId: String,
        progress: Int
    ) {
        challengeDao.updateProgress(
            challengeId = challengeId,
            progress = progress
        )
    }

    suspend fun completeChallenge(
        challengeId: String
    ) {
        challengeDao.completeChallenge(challengeId)
    }

    suspend fun markRewardClaimed(
        challengeId: String
    ) {
        challengeDao.markRewardClaimed(challengeId)
    }
}