package com.rpgahorro.savequest.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saving_goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                targetAmount INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goal_contributions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                goalId INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                date INTEGER NOT NULL,
                FOREIGN KEY(goalId) REFERENCES saving_goals(id)
                    ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_contributions_goalId ON goal_contributions(goalId)")
    }
}
