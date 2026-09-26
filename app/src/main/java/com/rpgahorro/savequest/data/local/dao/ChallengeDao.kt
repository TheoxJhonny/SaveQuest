package com.rpgahorro.savequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rpgahorro.savequest.data.local.entity.ChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {

    @Query("SELECT * FROM challenges")
    fun observeChallenges(): Flow<List<ChallengeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(challenge: ChallengeEntity)

    @Query(
        """
        UPDATE challenges
        SET progress = :progress
        WHERE id = :challengeId
        """
    )
    suspend fun updateProgress(
        challengeId: String,
        progress: Int
    )

    @Query(
        """
        UPDATE challenges
        SET completed = 1
        WHERE id = :challengeId
        """
    )
    suspend fun completeChallenge(
        challengeId: String
    )

    @Query(
        """
        UPDATE challenges
        SET rewardClaimed = 1
        WHERE id = :challengeId
        """
    )
    suspend fun markRewardClaimed(
        challengeId: String
    )
}