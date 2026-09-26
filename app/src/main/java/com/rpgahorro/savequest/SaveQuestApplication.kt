package com.rpgahorro.savequest

import android.app.Application
import androidx.room.Room
import com.rpgahorro.savequest.data.local.SaveQuestDatabase
import com.rpgahorro.savequest.data.repository.TransactionRepository

class SaveQuestApplication : Application() {

    lateinit var database: SaveQuestDatabase
        private set

    lateinit var transactionRepository: TransactionRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            SaveQuestDatabase::class.java,
            "savequest.db"
        ).build()

        transactionRepository = TransactionRepository(
            database.transactionDao()
        )
    }
}