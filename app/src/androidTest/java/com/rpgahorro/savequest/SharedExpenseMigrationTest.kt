package com.rpgahorro.savequest

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.*
import com.rpgahorro.savequest.data.repository.ContactRepository
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SharedExpenseMigrationTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun migrationFromV6PreservesAllDataAndContacts() = runBlocking {
        val name = "contacts-migration-${UUID.randomUUID()}.db"
        val fixture =
            InstrumentationRegistry.getInstrumentation()
                .context
                .assets
                .open("com.rpgahorro.savequest.data.local.SaveQuestDatabase/6.json")
                .bufferedReader()
                .use { JSONObject(it.readText()).getJSONObject("database") }
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        val before = mutableMapOf<String, List<List<String?>>>()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val entities = fixture.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) db.execSQL(
                    indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table)
                )
            }
            val setup = fixture.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO accounts VALUES (1, 'Principal', 1)")
            db.execSQL("INSERT INTO accounts VALUES (2, 'Efectivo', 2)")
            db.execSQL(
                "INSERT INTO transactions (id,type,amount,category,description,date,accountId,latitude,longitude) VALUES (7,'INCOME',60000,'Pago','Original',123,2,-33.45,-70.66)"
            )
            db.execSQL(
                "INSERT INTO transactions (id,type,amount,category,description,date,accountId) VALUES (8,'INCOME',60000,'Pago','Original',123,2)"
            )
            db.execSQL("INSERT INTO saving_goals VALUES (4, 'Viaje', 100000, 122)")
            db.execSQL("INSERT INTO goal_contributions VALUES (9, 4, 10000, 125, 2)")
            db.execSQL("INSERT INTO user_progress VALUES (1, 130)")
            db.execSQL(
                "INSERT INTO challenges VALUES ('track_3_transactions','Mantén el ritmo','Registrar',3,3,100,1,1)"
            )
            db.execSQL("INSERT INTO contacts VALUES (42, 'Original', '123', 'Nota', 1, 10)")
            for (i in 0 until entities.length()) {
                val table = entities.getJSONObject(i).getString("tableName")
                db.rawQuery("SELECT * FROM $table ORDER BY 1", null).use {
                    before[table] = rows(it)
                }
            }
            db.version = 6
        }
        val db =
            Room.databaseBuilder(context, SaveQuestDatabase::class.java, name)
                .addMigrations(MIGRATION_6_7, com.rpgahorro.savequest.data.local.MIGRATION_7_8)
                .addCallback(DatabaseDefaults)
                .build()
        try {
            assertEquals("Original", db.contactDao().find(42)!!.name)
            before.forEach { (table, expected) ->
                db.openHelper.readableDatabase.query("SELECT * FROM $table ORDER BY 1").use {
                    assertEquals(
                        table,
                        expected,
                        rows(it).map { row -> row.take(expected.firstOrNull()?.size ?: row.size) },
                    )
                }
            }
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use {
                assertFalse(it.moveToFirst())
            }
            val id = ContactRepository(db.contactDao()).save(null, "Ana", "", "")
            assertNotNull(db.contactDao().find(id))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    private fun rows(cursor: android.database.Cursor): List<List<String?>> = buildList {
        while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) })
    }
}
