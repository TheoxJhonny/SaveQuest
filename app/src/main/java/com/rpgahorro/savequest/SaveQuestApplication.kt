package com.rpgahorro.savequest

import com.rpgahorro.savequest.data.local.MIGRATION_3_4
import com.rpgahorro.savequest.data.repository.SavingGoalRepository
import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rpgahorro.savequest.data.local.SaveQuestDatabase
import com.rpgahorro.savequest.data.repository.TransactionRepository
import com.rpgahorro.savequest.data.repository.UserProgressRepository
import com.rpgahorro.savequest.data.repository.ChallengeRepository




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

private val MIGRATION_2_3 = object : Migration(2, 3) {

    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS challenges (
                id TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                target INTEGER NOT NULL,
                progress INTEGER NOT NULL,
                xpReward INTEGER NOT NULL,
                completed INTEGER NOT NULL,
                rewardClaimed INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT OR IGNORE INTO challenges (
                id,
                title,
                description,
                target,
                progress,
                xpReward,
                completed,
                rewardClaimed
            )
            VALUES (
                'track_3_transactions',
                'Mantén el ritmo',
                'Registra 3 movimientos',
                3,
                0,
                100,
                0,
                0
            )
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

    lateinit var challengeRepository: ChallengeRepository
        private set


    lateinit var savingGoalRepository: SavingGoalRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Crear la base de datos Room
        database = Room.databaseBuilder(
            applicationContext,
            SaveQuestDatabase::class.java,
            "savequest.db"
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4
            )
            .build()


        // 2. Crear el repositorio de movimientos
        transactionRepository = TransactionRepository(
            database.transactionDao()
        )


        // 3. Crear el repositorio de progreso / XP
        userProgressRepository = UserProgressRepository(
            database.userProgressDao()
        )

        savingGoalRepository = SavingGoalRepository(database.savingGoalDao())

        challengeRepository = ChallengeRepository(
            database.challengeDao()
        )
    }
}