package com.rpgahorro.savequest.data.local

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
        ChallengeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SaveQuestDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun userProgressDao(): UserProgressDao

    abstract fun challengeDao(): ChallengeDao

}