package com.rpgahorro.savequest.data.local.dao

import androidx.room.*
import com.rpgahorro.savequest.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedExpenseDao {
    @Transaction
    @Query("SELECT * FROM shared_expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<SharedExpenseDetails>>

    @Transaction
    @Query("SELECT * FROM shared_expenses WHERE id = :id")
    suspend fun find(id: String): SharedExpenseDetails?

    @Insert suspend fun insert(expense: SharedExpenseEntity)
    @Insert suspend fun insertItems(items: List<SharedItemEntity>)
    @Insert suspend fun insertShares(shares: List<SharedShareEntity>)
}
