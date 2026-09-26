package com.rpgahorro.savequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rpgahorro.savequest.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {

    @Query(
        """
        SELECT *
        FROM user_progress
        WHERE id = 1
        LIMIT 1
        """
    )
    fun observeProgress(): Flow<UserProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(
        progress: UserProgressEntity
    )

    @Query(
        """
        UPDATE user_progress
        SET totalXp = totalXp + :amount
        WHERE id = 1
        """
    )
    suspend fun addXp(amount: Int)
}