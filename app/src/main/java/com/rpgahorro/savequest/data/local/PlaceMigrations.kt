package com.rpgahorro.savequest.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 =
    object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN placeId TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN placeName TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN placeCategory TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN placeAddress TEXT")
            db.execSQL("CREATE INDEX index_transactions_placeId ON transactions(placeId)")
        }
    }
