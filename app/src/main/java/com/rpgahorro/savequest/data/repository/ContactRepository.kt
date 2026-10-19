package com.rpgahorro.savequest.data.repository

import com.rpgahorro.savequest.data.local.dao.ContactDao
import com.rpgahorro.savequest.data.local.entity.ContactEntity

class ContactRepository(private val dao: ContactDao) {
    val contacts = dao.observeAll()

    suspend fun save(id: Long?, name: String, phone: String, notes: String): Long {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanNotes = notes.trim()
        require(cleanName.length in 1..80) { "El nombre debe tener entre 1 y 80 caracteres." }
        require(cleanPhone.length <= 30) { "El teléfono no puede superar los 30 caracteres." }
        require(cleanNotes.length <= 500) { "La nota no puede superar los 500 caracteres." }
        return if (id == null) dao.insert(
            ContactEntity(
                name = cleanName,
                phone = cleanPhone,
                notes = cleanNotes
            )
        )
        else {
            require(
                dao.updateDetails(
                    id,
                    cleanName,
                    cleanPhone,
                    cleanNotes
                ) == 1
            ) { "El contacto ya no existe." }
            id
        }
    }

    suspend fun archive(id: Long, archived: Boolean) {
        require(dao.setArchived(id, archived) == 1) { "El contacto ya no existe." }
    }
}
