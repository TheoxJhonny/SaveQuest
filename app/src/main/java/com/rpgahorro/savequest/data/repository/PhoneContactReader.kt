package com.rpgahorro.savequest.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhoneContactDraft(val name: String, val phone: String)

/** Lee solo el teléfono elegido con el permiso temporal del selector de Android. */
class PhoneContactReader(private val resolver: ContentResolver) {
    suspend fun read(uri: Uri): PhoneContactDraft =
        withContext(Dispatchers.IO) {
            require(uri.scheme == ContentResolver.SCHEME_CONTENT) {
                "El contacto seleccionado no es válido."
            }
            resolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)
                ?.use { cursor ->
                    require(cursor.moveToFirst()) { "El contacto ya no está disponible." }
                    PhoneContactDraft(
                        cursor.getString(cursor.getColumnIndexOrThrow(Phone.DISPLAY_NAME))
                            .orEmpty(),
                        cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER)).orEmpty(),
                    )
                } ?: throw IllegalArgumentException("No se pudo leer el contacto seleccionado.")
        }
}
