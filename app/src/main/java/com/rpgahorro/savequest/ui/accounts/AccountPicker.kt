package com.rpgahorro.savequest.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rpgahorro.savequest.data.local.entity.AccountBalance
import com.rpgahorro.savequest.ui.components.money
import com.rpgahorro.savequest.ui.theme.*

@Composable
fun AccountPicker(
    accounts: List<AccountBalance>,
    selectedId: Long,
    enabled: Boolean,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.account.id == selectedId }
    Column {
        Box {
            OutlinedButton(
                { expanded = true },
                enabled = enabled && accounts.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cuenta: ${selected?.account?.name ?: "Selecciona una cuenta"} ▾")
            }
            DropdownMenu(expanded, { expanded = false }) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.account.name) },
                        onClick = { onSelect(account.account.id); expanded = false })
                }
            }
        }
        selected?.let {
            Text(
                "Disponible: ${money(it.available)}",
                color = if (it.available < 0) SqExpense else SqTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
