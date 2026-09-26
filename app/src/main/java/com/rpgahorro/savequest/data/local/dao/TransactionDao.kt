package com.rpgahorro.savequest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(
        transaction: TransactionEntity
    )

    @Update
    suspend fun update(
        transaction: TransactionEntity
    )

    @Delete
    suspend fun delete(
        transaction: TransactionEntity
    )

    @Query(
        """
        SELECT *
        FROM transactions
        ORDER BY date DESC
        """
    )
    fun getAll(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: Long): TransactionEntity?
}