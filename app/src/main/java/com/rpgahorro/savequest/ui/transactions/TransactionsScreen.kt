package com.rpgahorro.savequest.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    accounts: List<com.rpgahorro.savequest.data.local.entity.AccountBalance> = emptyList(),
) {
    var filter by rememberSaveable { mutableStateOf("Todos") }
    val visible =
        transactions.filter {
            when (filter) {
                "Ingresos" -> it.type == "INCOME"
                "Gastos" -> it.type == "EXPENSE"
                else -> true
            }
        }
    val dateFormat = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.forLanguageTag("es-CL"))
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            QuestLabel("TU REGISTRO")
            Text(
                "Movimientos",
                style = MaterialTheme.typography.headlineLarge,
                color = SqTextPrimary,
            )
            Text("Cada decisión forma parte de tu aventura.", color = SqTextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todos", "Ingresos", "Gastos").forEach { label ->
                    FilterChip(filter == label, { filter = label }, label = { Text(label) })
                }
            }
        }
        if (visible.isEmpty())
            item {
                QuestPanel {
                    Text("Todavía no hay movimientos en esta vista.", color = SqTextSecondary)
                }
            }
        items(visible, key = { it.id }) { transaction ->
            QuestPanel {
                TransactionItem(
                    transaction.description.ifBlank {
                        transaction.placeName ?: transaction.category
                    },
                    transaction.category,
                    transaction.amount,
                    transaction.type == TransactionType.INCOME.name,
                )
                if (transaction.placeName != null && transaction.description.isNotBlank()) {
                    Text(
                        transaction.placeName,
                        color = SqPrimarySoft,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    accounts.firstOrNull { it.account.id == transaction.accountId }?.account?.name
                        ?: "Cuenta",
                    color = SqPrimarySoft,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    dateFormat.format(
                        Instant.ofEpochMilli(transaction.date).atZone(ZoneId.systemDefault())
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SqTextSecondary,
                )
            }
        }
    }
}
