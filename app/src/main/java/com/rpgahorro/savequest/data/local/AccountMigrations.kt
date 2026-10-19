package com.rpgahorro.savequest.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room ejecuta la migración en una transacción; se conservan IDs, importes e historial. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT COLLATE NOCASE NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX index_accounts_name ON accounts(name)")
        db.execSQL("INSERT INTO accounts (id, name, createdAt) VALUES (1, 'Cuenta principal', 0)")

        db.execSQL(
            """
            CREATE TABLE transactions_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL, amount INTEGER NOT NULL, category TEXT NOT NULL,
                description TEXT NOT NULL, date INTEGER NOT NULL, goalId INTEGER,
                latitude REAL, longitude REAL, accountId INTEGER NOT NULL,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO transactions_new (id, type, amount, category, description, date, goalId, latitude, longitude, accountId)
            SELECT id, type, amount, category, description, date, goalId, latitude, longitude, 1 FROM transactions
        """.trimIndent()
        )
        db.execSQL("DROP TABLE transactions")
        db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        db.execSQL("CREATE INDEX index_transactions_accountId ON transactions(accountId)")

        db.execSQL(
            """
            CREATE TABLE goal_contributions_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, goalId INTEGER NOT NULL,
                amount INTEGER NOT NULL, date INTEGER NOT NULL, accountId INTEGER NOT NULL,
                FOREIGN KEY(goalId) REFERENCES saving_goals(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO goal_contributions_new (id, goalId, amount, date, accountId)
            SELECT id, goalId, amount, date, 1 FROM goal_contributions
        """.trimIndent()
        )
        db.execSQL("DROP TABLE goal_contributions")
        db.execSQL("ALTER TABLE goal_contributions_new RENAME TO goal_contributions")
        db.execSQL("CREATE INDEX index_goal_contributions_goalId ON goal_contributions(goalId)")
        db.execSQL("CREATE INDEX index_goal_contributions_accountId ON goal_contributions(accountId)")
    }
}
