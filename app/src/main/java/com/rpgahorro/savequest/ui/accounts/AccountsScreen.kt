package com.rpgahorro.savequest.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.theme.*

@Composable
fun AccountsScreen(viewModel: AccountsViewModel, onBack: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(viewModel.created) {
        if (viewModel.created) { creating = false; name = ""; viewModel.clearStatus() }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
            QuestLabel("ORGANIZA TU DINERO")
            Text("Mis cuentas", style = MaterialTheme.typography.headlineLarge, color = SqTextPrimary)
            Text("Separa efectivo, ahorros y otras cuentas.", color = SqTextSecondary)
            QuestButton("+  Nueva cuenta", { viewModel.clearStatus(); creating = true })
        }
        items(accounts, key = { it.account.id }) { balance ->
            QuestPanel {
                Text(balance.account.name, style = MaterialTheme.typography.titleLarge)
                Text(money(balance.balance), style = MaterialTheme.typography.headlineMedium)
                Text("Reservado: ${money(balance.reserved)}", color = SqPrimarySoft)
                Text("Disponible: ${money(balance.available)}", color = if (balance.available < 0) SqExpense else SqSavings)
            }
        }
    }
    if (creating) AlertDialog(
        onDismissRequest = { if (!viewModel.saving) creating = false },
        title = { Text("Nueva cuenta") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, enabled = !viewModel.saving, label = { Text("Nombre de la cuenta") }, singleLine = true)
                Text("Comienza en cero. Registra un ingreso para añadir saldo.", color = SqTextSecondary)
                viewModel.error?.let { Text(it, color = SqExpense) }
            }
        },
        confirmButton = { TextButton({ viewModel.create(name) }, enabled = !viewModel.saving) { Text(if (viewModel.saving) "Guardando…" else "Crear") } },
        dismissButton = { TextButton({ creating = false }, enabled = !viewModel.saving) { Text("Cancelar") } })
}
