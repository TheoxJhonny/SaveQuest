package com.rpgahorro.savequest

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rpgahorro.savequest.data.local.*
import com.rpgahorro.savequest.data.repository.ContactRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ContactDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun migrationFromV5PreservesEveryExistingTable() = runBlocking {
        val name = "contacts-migration-${UUID.randomUUID()}.db"
        val fixture = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.rpgahorro.savequest.data.local.SaveQuestDatabase/5.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
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
                for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = fixture.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO accounts VALUES (1, 'Principal', 1)")
            db.execSQL("INSERT INTO accounts VALUES (2, 'Efectivo', 2)")
            db.execSQL("INSERT INTO transactions (id,type,amount,category,description,date,accountId,latitude,longitude) VALUES (7,'INCOME',60000,'Pago','Original',123,2,-33.45,-70.66)")
            db.execSQL("INSERT INTO transactions (id,type,amount,category,description,date,accountId) VALUES (8,'INCOME',60000,'Pago','Original',123,2)")
            db.execSQL("INSERT INTO saving_goals VALUES (4, 'Viaje', 100000, 122)")
            db.execSQL("INSERT INTO goal_contributions VALUES (9, 4, 10000, 125, 2)")
            db.execSQL("INSERT INTO user_progress VALUES (1, 130)")
            db.execSQL("INSERT INTO challenges VALUES ('track_3_transactions','Mantén el ritmo','Registrar',3,3,100,1,1)")
            for (i in 0 until entities.length()) {
                val table = entities.getJSONObject(i).getString("tableName")
                db.rawQuery("SELECT * FROM $table ORDER BY 1", null).use { before[table] = rows(it) }
            }
            db.version = 5
        }
        val db = Room.databaseBuilder(context, SaveQuestDatabase::class.java, name)
            .addMigrations(MIGRATION_5_6).addCallback(DatabaseDefaults).build()
        try {
            assertTrue(db.contactDao().observeAll().first().isEmpty())
            before.forEach { (table, expected) -> db.openHelper.readableDatabase.query("SELECT * FROM $table ORDER BY 1").use { assertEquals(table, expected, rows(it)) } }
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            val id = ContactRepository(db.contactDao()).save(null, "Ana", "", "")
            assertNotNull(db.contactDao().find(id))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun editAndArchiveKeepIdentityAndDoNotChangeMoneyOrXp() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, SaveQuestDatabase::class.java).addCallback(DatabaseDefaults).build()
        try {
            val repository = ContactRepository(db.contactDao())
            val id = repository.save(null, "  José  ", "+56 9 1234 5678", "Amigo")
            val original = db.contactDao().find(id)!!
            repository.archive(id, true)
            assertEquals(id, repository.save(id, "José Pérez", "", "Nueva nota"))
            val edited = db.contactDao().find(id)!!
            assertTrue(edited.archived)
            assertEquals(original.createdAt, edited.createdAt)
            repository.archive(id, false)
            assertFalse(db.contactDao().find(id)!!.archived)
            val other = repository.save(null, "José Pérez", "Otro teléfono", "")
            assertNotEquals(id, other)
            try { repository.save(id, " ", "", ""); fail("Nombre vacío") } catch (_: IllegalArgumentException) { }
            try { repository.save(99999, "Ana", "", ""); fail("Contacto inexistente") } catch (_: IllegalArgumentException) { }
            assertEquals(2, repository.contacts.first().size)
            assertTrue(db.transactionDao().getAll().first().isEmpty())
            assertEquals(0, db.userProgressDao().observeProgress().first()!!.totalXp)
        } finally { db.close() }
    }

    private fun rows(cursor: android.database.Cursor): List<List<String?>> = buildList {
        while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) })
    }
}
