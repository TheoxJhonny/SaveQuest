package com.rpgahorro.savequest.ui.goals

import com.rpgahorro.savequest.data.local.entity.AccountBalance
import com.rpgahorro.savequest.ui.accounts.AccountPicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.theme.*

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    available: Long,
    initialGoalId: Long? = null,
    onInitialGoalHandled: () -> Unit = {}
) {
    val accounts by viewModel.accounts.collectAsState()
    val goals by viewModel.goals.collectAsState()
    var creating by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(initialGoalId) {
        if (initialGoalId != null) {
            selectedId = initialGoalId
            viewModel.clearError()
            onInitialGoalHandled()
        }
    }
    val selected = goals.firstOrNull { it.goal.id == selectedId }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            QuestLabel("TUS OBJETIVOS")
            Text(
                "Metas de ahorro",
                style = MaterialTheme.typography.headlineLarge,
                color = SqTextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text("Cada aporte te acerca a tu próxima conquista.", color = SqTextSecondary)
        }
        item {
            QuestPanel {
                Text("Disponible para reservar", color = SqTextSecondary)
                Text(
                    money(available),
                    style = MaterialTheme.typography.headlineMedium,
                    color = SqSavings
                )
                Text("Un aporte reserva tu dinero; no es un gasto.", color = SqTextSecondary)
                QuestButton("+  Nueva meta", { viewModel.clearError(); creating = true })
            }
        }
        if (goals.isEmpty()) item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QuestIllustration(QuestArt.Treasure, Modifier.size(100.dp))
                Text("Tu próxima aventura comienza con una meta.", color = SqTextSecondary)
            }
        }
        items(goals, key = { it.goal.id }) { goal ->
            SavingGoalCard(
                goal.goal.name,
                goal.savedAmount,
                goal.goal.targetAmount,
                { viewModel.clearError(); selectedId = goal.goal.id },
                heading = "OBJETIVO DE AHORRO"
            )
        }
    }
    if (creating || selected != null) {
        GoalEditor(
            key = if (creating) "new" else "goal-$selectedId",
            title = if (creating) "Nueva meta" else "Aportar a ${selected!!.goal.name}",
            creating = creating,
            accounts = accounts,
            saving = viewModel.saving,
            error = viewModel.error,
            hint = if (creating) "Elige algo que quieras alcanzar." else "Disponible total: ${
                money(
                    available
                )
            }\nFaltan: ${money(selected!!.goal.targetAmount - selected.savedAmount)}",
            onDismiss = {
                if (!viewModel.saving) {
                    creating = false; selectedId = null; viewModel.clearError()
                }
            },
            onSave = { name, amount, accountId ->
                val done = { creating = false; selectedId = null }
                if (creating) viewModel.create(name, amount, done)
                else selectedId?.let { viewModel.contribute(it, amount, accountId, done) }
            }
        )
    }
}

@Composable
private fun GoalEditor(
    key: String,
    title: String,
    creating: Boolean,
    accounts: List<AccountBalance>,
    saving: Boolean,
    error: String?,
    hint: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Long) -> Unit
) {
    var accountId by rememberSaveable(key) { mutableLongStateOf(1L) }
    var name by rememberSaveable(key) { mutableStateOf("") }
    var amount by rememberSaveable(key) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SqSurface,
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(hint, color = SqTextSecondary)
                if (!creating) AccountPicker(accounts, accountId, !saving) { accountId = it }
                if (creating) OutlinedTextField(
                    name,
                    { name = it },
                    enabled = !saving,
                    label = { Text("Nombre de la meta") },
                    singleLine = true
                )
                OutlinedTextField(
                    amount,
                    { amount = it },
                    enabled = !saving,
                    label = { Text(if (creating) "Objetivo en pesos" else "Aporte en pesos") },
                    supportingText = { Text("Sin puntos ni comas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (error != null) Text(error, color = SqExpense)
            }
        },
        confirmButton = {
            TextButton(
                { onSave(name, amount, accountId) },
                enabled = !saving && (creating || accounts.any { it.account.id == accountId })
            ) { Text(if (saving) "Guardando…" else "Guardar") }
        },
        dismissButton = { TextButton(onDismiss, enabled = !saving) { Text("Cancelar") } })
}
