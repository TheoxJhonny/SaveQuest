package com.rpgahorro.savequest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rpgahorro.savequest.data.local.dao.TransactionDao
import com.rpgahorro.savequest.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SaveQuestDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
}