package com.rpgahorro.savequest.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS shared_expenses (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, date INTEGER NOT NULL, payerContactId INTEGER, accountId INTEGER, total INTEGER NOT NULL, ownExpense INTEGER NOT NULL, cashOutflow INTEGER NOT NULL, FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(payerContactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_shared_expenses_accountId ON shared_expenses(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_shared_expenses_payerContactId ON shared_expenses(payerContactId)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS shared_items (expenseId TEXT NOT NULL, position INTEGER NOT NULL, name TEXT NOT NULL, quantity INTEGER NOT NULL, unitPrice INTEGER NOT NULL, PRIMARY KEY(expenseId,position), FOREIGN KEY(expenseId) REFERENCES shared_expenses(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS shared_shares (expenseId TEXT NOT NULL, participantId INTEGER NOT NULL, contactId INTEGER, name TEXT NOT NULL, amount INTEGER NOT NULL, PRIMARY KEY(expenseId,participantId), FOREIGN KEY(expenseId) REFERENCES shared_expenses(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_shared_shares_contactId ON shared_shares(contactId)")
    }
}
