package com.rpgahorro.savequest.ui.contacts

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.rpgahorro.savequest.data.repository.PhoneContactDraft
import com.rpgahorro.savequest.data.repository.PhoneContactReader
import kotlinx.coroutines.CancellationException

data class PhoneContactPickerState(
    val draft: PhoneContactDraft?,
    val error: String?,
    val reading: Boolean,
    val launch: () -> Unit,
    val consume: () -> Unit,
)

/** Mantener fuera del acceso protegido: el resultado debe sobrevivir al bloqueo al salir. */
@Composable
fun rememberPhoneContactPicker(): PhoneContactPickerState {
    val context = LocalContext.current
    val reader = remember(context) { PhoneContactReader(context.contentResolver) }
    var uri by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    var phone by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                uri = result.data?.data?.toString()
                if (uri == null)
                    error = "El selector no devolvió un contacto. Puedes escribirlo manualmente."
            }
        }
    LaunchedEffect(uri) {
        val selected = uri ?: return@LaunchedEffect
        try {
            val contact = reader.read(Uri.parse(selected))
            phone = contact.phone
            name = contact.name
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            error = "No se pudo leer el contacto. Vuelve a elegirlo o escríbelo manualmente."
        }
        uri = null
    }
    return PhoneContactPickerState(
        draft = name?.let { PhoneContactDraft(it, phone) },
        error = error,
        reading = uri != null,
        launch = {
            error = null
            try {
                launcher.launch(Intent(Intent.ACTION_PICK).apply { type = Phone.CONTENT_TYPE })
            } catch (_: ActivityNotFoundException) {
                error =
                    "No hay una aplicación de contactos disponible. Puedes crear el contacto manualmente."
            } catch (_: SecurityException) {
                error =
                    "Android no permitió abrir el selector. Puedes crear el contacto manualmente."
            }
        },
        consume = {
            name = null
            phone = ""
            error = null
        },
    )
}
