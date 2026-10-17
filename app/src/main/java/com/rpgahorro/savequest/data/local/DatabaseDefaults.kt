package com.rpgahorro.savequest.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** Idempotente: completa datos iniciales sin reiniciar XP ni desafíos existentes. */
object DatabaseDefaults : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.beginTransaction()
        try {
            db.execSQL("INSERT OR IGNORE INTO user_progress (id, totalXp) VALUES (1, 0)")
            db.execSQL("""
                INSERT OR IGNORE INTO challenges
                (id, title, description, target, progress, xpReward, completed, rewardClaimed)
                VALUES ('track_3_transactions', 'Mantén el ritmo', 'Registra 3 movimientos', 3, 0, 100, 0, 0)
            """.trimIndent())
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
