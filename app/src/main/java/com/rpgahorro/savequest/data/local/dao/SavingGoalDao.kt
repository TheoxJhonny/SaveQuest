package com.rpgahorro.savequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.rpgahorro.savequest.data.local.entity.GoalContributionEntity
import com.rpgahorro.savequest.data.local.entity.SavingGoalEntity
import com.rpgahorro.savequest.data.local.entity.SavingGoalWithContributions
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SavingGoalDao {
    @Transaction
    @Query("SELECT * FROM saving_goals ORDER BY createdAt DESC, id DESC")
    abstract fun observeGoals(): Flow<List<SavingGoalWithContributions>>

    @Insert protected abstract suspend fun insertGoal(goal: SavingGoalEntity): Long

    @Insert
    protected abstract suspend fun insertContribution(contribution: GoalContributionEntity): Long

    @Query("SELECT * FROM saving_goals WHERE id = :goalId")
    protected abstract suspend fun findGoal(goalId: Long): SavingGoalEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM goal_contributions WHERE goalId = :goalId")
    protected abstract suspend fun savedAmount(goalId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(CASE
            WHEN type = 'INCOME' THEN amount
            WHEN type = 'EXPENSE' THEN -amount
            ELSE 0 END), 0)
            - (SELECT COALESCE(SUM(cashOutflow), 0) FROM shared_expenses WHERE accountId = :accountId)
            - (SELECT COALESCE(SUM(amount), 0) FROM goal_contributions WHERE accountId = :accountId)
        FROM transactions WHERE accountId = :accountId
    """
    )
    protected abstract suspend fun availableAmount(accountId: Long): Long

    @Query("DELETE FROM goal_contributions WHERE goalId = :goalId")
    protected abstract suspend fun deleteContributions(goalId: Long)

    @Query("DELETE FROM saving_goals WHERE id = :goalId")
    protected abstract suspend fun deleteGoalRow(goalId: Long): Int

    // Liberar reservas no modifica movimientos ni crea un ingreso. Ambas eliminaciones son
    // atómicas.
    @Transaction
    open suspend fun deleteGoal(goalId: Long) {
        requireNotNull(findGoal(goalId)) { "La meta ya no existe." }
        deleteContributions(goalId)
        check(deleteGoalRow(goalId) == 1)
    }

    suspend fun createGoal(name: String, targetAmount: Long): Long {
        require(name.isNotBlank()) { "Escribe un nombre para la meta." }
        require(targetAmount > 0) { "El objetivo debe ser mayor que cero." }
        return insertGoal(SavingGoalEntity(name = name.trim(), targetAmount = targetAmount))
    }

    // La comprobación y la reserva son atómicas, incluso con dos aportes simultáneos.
    @Transaction
    open suspend fun contribute(goalId: Long, amount: Long, accountId: Long = 1): Long {
        require(amount > 0) { "El aporte debe ser mayor que cero." }
        val goal = requireNotNull(findGoal(goalId)) { "La meta ya no existe." }
        require(amount <= goal.targetAmount - savedAmount(goalId)) {
            "El aporte supera el monto que falta para completar la meta."
        }
        require(amount <= availableAmount(accountId)) { "No tienes suficiente dinero disponible." }
        // Reservar dinero no crea un gasto ni reduce el patrimonio.
        return insertContribution(
            GoalContributionEntity(goalId = goalId, amount = amount, accountId = accountId)
        )
    }
}
