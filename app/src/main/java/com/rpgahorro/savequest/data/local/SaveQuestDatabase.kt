package com.rpgahorro.savequest.data.local

import com.rpgahorro.savequest.data.local.dao.SavingGoalDao
import com.rpgahorro.savequest.data.local.entity.SavingGoalEntity
import com.rpgahorro.savequest.data.local.entity.GoalContributionEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import com.rpgahorro.savequest.data.local.dao.TransactionDao
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.data.local.dao.UserProgressDao
import com.rpgahorro.savequest.data.local.entity.UserProgressEntity
import com.rpgahorro.savequest.data.local.dao.ChallengeDao
import com.rpgahorro.savequest.data.local.entity.ChallengeEntity

@Database(
    entities = [
        TransactionEntity::class,
        UserProgressEntity::class,
        ChallengeEntity::class,
        SavingGoalEntity::class,
        GoalContributionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SaveQuestDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun userProgressDao(): UserProgressDao

    abstract fun challengeDao(): ChallengeDao

    abstract fun savingGoalDao(): SavingGoalDao

}