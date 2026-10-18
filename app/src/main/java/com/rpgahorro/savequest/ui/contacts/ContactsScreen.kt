package com.rpgahorro.savequest.ui.contacts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.theme.*

@Composable
fun ContactsScreen(viewModel: ContactsViewModel, onBack: () -> Unit) {
    val contacts by viewModel.contacts.collectAsState()
    var search by rememberSaveable { mutableStateOf("") }
    var archived by rememberSaveable { mutableStateOf(false) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var archiveId by rememberSaveable { mutableStateOf<Long?>(null) }
    val busy = viewModel.saving || viewModel.saved
    val selected = contacts?.firstOrNull { it.id == archiveId }
    val visible = contacts.orEmpty().filter { it.archived == archived &&
        (it.name.contains(search.trim(), ignoreCase = true) || it.phone.contains(search.trim())) }

    LaunchedEffect(viewModel.saved) {
        if (viewModel.saved) { editorOpen = false; archiveId = null; viewModel.clearStatus() }
    }
    BackHandler(enabled = busy) { }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack, enabled = !busy) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
                QuestLabel("TU EQUIPO")
            }
            Text("Contactos", style = MaterialTheme.typography.headlineLarge, color = SqTextPrimary)
            Text("Personas con quienes compartes compras y gastos.", color = SqTextSecondary)
        }
        item { QuestButton("Nuevo contacto", {
            viewModel.clearStatus(); editingId = null; name = ""; phone = ""; notes = ""; editorOpen = true
        }, enabled = !busy) }
        item {
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Buscar por nombre o teléfono") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!archived, { archived = false }, label = { Text("Activos") })
                FilterChip(archived, { archived = true }, label = { Text("Archivados") })
            }
        }
        item {
            when {
                viewModel.loadError != null -> Text(viewModel.loadError!!, color = SqExpense)
                contacts == null -> CircularProgressIndicator()
                visible.isEmpty() -> QuestPanel {
                    Icon(Icons.Outlined.Groups, null, tint = SqPrimarySoft)
                    Text(if (search.isNotBlank()) "No se encontraron contactos." else if (archived) "No tienes contactos archivados." else "Agrega a tu primer contacto.")
                }
            }
        }
        items(visible, key = { it.id }) { contact ->
            QuestPanel {
                Text(contact.name, style = MaterialTheme.typography.titleLarge)
                if (contact.phone.isNotBlank()) Text(contact.phone, color = SqTextSecondary)
                if (contact.notes.isNotBlank()) Text(contact.notes, color = SqTextSecondary)
                Row {
                    TextButton({
                        viewModel.clearStatus(); editingId = contact.id; name = contact.name; phone = contact.phone; notes = contact.notes; editorOpen = true
                    }, enabled = !busy) { Text("Editar") }
                    TextButton({ viewModel.clearStatus(); archiveId = contact.id }, enabled = !busy) {
                        Text(if (contact.archived) "Restaurar" else "Archivar")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if (editorOpen) AlertDialog(
        onDismissRequest = { if (!busy) { editorOpen = false; viewModel.clearStatus() } },
        title = { Text(if (editingId == null) "Nuevo contacto" else "Editar contacto") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, enabled = !busy, singleLine = true, label = { Text("Nombre") })
                OutlinedTextField(phone, { phone = it }, enabled = !busy, singleLine = true,
                    label = { Text("Teléfono (opcional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(notes, { notes = it }, enabled = !busy, label = { Text("Nota (opcional)") }, maxLines = 4)
                viewModel.error?.let { Text(it, color = SqExpense) }
            }
        },
        confirmButton = { TextButton({ viewModel.save(editingId, name, phone, notes) }, enabled = !busy) { Text(if (busy) "Guardando…" else "Guardar") } },
        dismissButton = { TextButton({ editorOpen = false; viewModel.clearStatus() }, enabled = !busy) { Text("Cancelar") } }
    )
    if (selected != null) AlertDialog(
        onDismissRequest = { if (!busy) { archiveId = null; viewModel.clearStatus() } },
        title = { Text(if (selected.archived) "Restaurar contacto" else "Archivar contacto") },
        text = { Column {
            Text(if (selected.archived) "${selected.name} volverá a tus contactos activos." else "${selected.name} dejará de aparecer entre los activos. Su información se conservará.")
            viewModel.error?.let { Text(it, color = SqExpense) }
        } },
        confirmButton = { TextButton({ viewModel.archive(selected.id, !selected.archived) }, enabled = !busy) { Text("Confirmar") } },
        dismissButton = { TextButton({ archiveId = null; viewModel.clearStatus() }, enabled = !busy) { Text("Cancelar") } }
    )
}
