package com.rpgahorro.savequest

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rpgahorro.savequest.data.local.SaveQuestDatabase
import com.rpgahorro.savequest.data.repository.TransactionRepository
import com.rpgahorro.savequest.data.repository.UserProgressRepository


private val MIGRATION_1_2 = object : Migration(1, 2) {

    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_progress (
                id INTEGER NOT NULL,
                totalXp INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT OR IGNORE INTO user_progress (id, totalXp)
            VALUES (1, 0)
            """.trimIndent()
        )
    }
}


class SaveQuestApplication : Application() {

    lateinit var database: SaveQuestDatabase
        private set

    lateinit var transactionRepository: TransactionRepository
        private set

    lateinit var userProgressRepository: UserProgressRepository
        private set


    override fun onCreate() {
        super.onCreate()

        // 1. Crear la base de datos Room
        database = Room.databaseBuilder(
            applicationContext,
            SaveQuestDatabase::class.java,
            "savequest.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()


        // 2. Crear el repositorio de movimientos
        transactionRepository = TransactionRepository(
            database.transactionDao()
        )


        // 3. Crear el repositorio de progreso / XP
        userProgressRepository = UserProgressRepository(
            database.userProgressDao()
        )
    }
}