package com.rpgahorro.savequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rpgahorro.savequest.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY archived, name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun find(id: Long): ContactEntity?

    @Insert
    suspend fun insert(contact: ContactEntity): Long

    @Query("UPDATE contacts SET name = :name, phone = :phone, notes = :notes WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, phone: String, notes: String): Int

    @Query("UPDATE contacts SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean): Int
}
