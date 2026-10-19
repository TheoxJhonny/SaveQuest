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
        com.rpgahorro.savequest.data.local.entity.SharedExpenseEntity::class,
        com.rpgahorro.savequest.data.local.entity.SharedItemEntity::class,
        com.rpgahorro.savequest.data.local.entity.SharedShareEntity::class,
        com.rpgahorro.savequest.data.local.entity.ContactEntity::class,
        com.rpgahorro.savequest.data.local.entity.AccountEntity::class,
        TransactionEntity::class,
        UserProgressEntity::class,
        ChallengeEntity::class,
        SavingGoalEntity::class,
        GoalContributionEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class SaveQuestDatabase : RoomDatabase() {
    abstract fun sharedExpenseDao(): com.rpgahorro.savequest.data.local.dao.SharedExpenseDao


    abstract fun contactDao(): com.rpgahorro.savequest.data.local.dao.ContactDao

    abstract fun accountDao(): com.rpgahorro.savequest.data.local.dao.AccountDao

    abstract fun transactionDao(): TransactionDao

    abstract fun userProgressDao(): UserProgressDao

    abstract fun challengeDao(): ChallengeDao

    abstract fun savingGoalDao(): SavingGoalDao

}