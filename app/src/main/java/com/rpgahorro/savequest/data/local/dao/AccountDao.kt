package com.rpgahorro.savequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rpgahorro.savequest.data.local.entity.AccountBalance
import com.rpgahorro.savequest.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AccountDao {
    @Query(
        """
        SELECT accounts.*,
            (SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount WHEN type = 'EXPENSE' THEN -amount ELSE 0 END), 0)
                FROM transactions WHERE accountId = accounts.id) - (SELECT COALESCE(SUM(cashOutflow), 0) FROM shared_expenses WHERE accountId = accounts.id) AS balance,
            (SELECT COALESCE(SUM(amount), 0) FROM goal_contributions WHERE accountId = accounts.id) AS reserved
        FROM accounts ORDER BY id
    """
    )
    abstract fun observeBalances(): Flow<List<AccountBalance>>

    @Query("SELECT EXISTS(SELECT 1 FROM accounts WHERE id = :id)")
    abstract suspend fun exists(id: Long): Boolean

    @Insert
    protected abstract suspend fun insert(account: AccountEntity): Long

    suspend fun create(name: String): Long {
        require(name.trim().length in 1..60) { "El nombre debe tener entre 1 y 60 caracteres." }
        return insert(AccountEntity(name = name.trim()))
    }
}
