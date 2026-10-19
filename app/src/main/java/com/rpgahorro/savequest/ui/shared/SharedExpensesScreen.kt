package com.rpgahorro.savequest.ui.shared

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rpgahorro.savequest.data.local.entity.*
import com.rpgahorro.savequest.domain.shared.*
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.theme.*
import java.util.UUID
import java.text.DateFormat
import java.util.Date

@Composable
fun SharedExpensesScreen(vm: SharedExpensesViewModel, onBack: () -> Unit, onContacts: () -> Unit) {
    val expenses by vm.expenses.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var editing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm.saved) {
        if (vm.saved) {
            editing = false; vm.clearStatus()
        }
    }
    BackHandler(editing) {
        if (!vm.saving) {
            editing = false; vm.clearStatus()
        }
    }
    if (editing) {
        SharedExpenseForm(
            vm, contacts.orEmpty().filter { !it.archived }, accounts.orEmpty(),
            { editing = false; vm.clearStatus() }, onContacts
        )
        return
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
                QuestLabel("MISIÓN EN EQUIPO")
            }
            Text("Gastos compartidos", style = MaterialTheme.typography.headlineLarge)
            Text("Tu parte, lo que pagaste y las deudas, por separado.", color = SqTextSecondary)
        }
        item {
            QuestButton(
                "Nuevo gasto compartido", { vm.clearStatus(); editing = true },
                enabled = expenses != null && contacts != null && accounts != null && vm.loadError == null
            )
        }
        item { TextButton(onContacts) { Text("Administrar contactos") } }
        item {
            when {
                vm.loadError != null -> Text(vm.loadError!!, color = SqExpense)
                expenses == null -> CircularProgressIndicator()
                expenses!!.isEmpty() -> QuestPanel { Text("Aún no has registrado gastos compartidos.") }
                else -> QuestPanel {
                    QuestLabel("DEUDAS POR CONTACTO")
                    val debts = mutableMapOf<Long, Pair<Long, Long>>()
                    expenses!!.forEach { detail ->
                        if (detail.expense.payerContactId == null) detail.shares.filter { it.contactId != null }
                            .forEach {
                                val old = debts[it.contactId!!] ?: (0L to 0L)
                                debts[it.contactId] = (old.first + it.amount) to old.second
                            } else {
                            val id = detail.expense.payerContactId
                            val old = debts[id] ?: (0L to 0L)
                            debts[id] = old.first to (old.second + detail.expense.ownExpense)
                        }
                    }
                    debts.filterValues { it.first > 0 || it.second > 0 }.forEach { (id, amounts) ->
                        val name =
                            contacts.orEmpty().firstOrNull { it.id == id }?.name ?: "Contacto #$id"
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Te debe ${money(amounts.first)} · Debes ${money(amounts.second)}",
                            color = SqTextSecondary
                        )
                    }
                    Text(
                        "Saldos de las compras registradas.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        items(expenses.orEmpty(), key = { it.expense.id }) { detail ->
            var expanded by rememberSaveable(detail.expense.id) { mutableStateOf(false) }
            QuestPanel {
                Text(detail.expense.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    DateFormat.getDateTimeInstance(
                        DateFormat.SHORT,
                        DateFormat.SHORT,
                        java.util.Locale.forLanguageTag("es-CL")
                    ).format(Date(detail.expense.date)), color = SqTextSecondary
                )
                Text("Total: ${money(detail.expense.total)} · Tu parte: ${money(detail.expense.ownExpense)}")
                val payer =
                    detail.shares.firstOrNull { it.contactId == detail.expense.payerContactId }?.name
                        ?: "Yo"
                Text(
                    "Pagó: $payer · Salió de tus cuentas: ${money(detail.expense.cashOutflow)}",
                    color = SqTextSecondary
                )
                TextButton({
                    expanded = !expanded
                }) { Text(if (expanded) "Ocultar detalle" else "Ver productos y reparto") }
                if (expanded) {
                    detail.items.sortedBy { it.position }
                        .forEach { Text("${it.quantity} × ${it.name} · ${money(it.unitPrice)} c/u") }
                    HorizontalDivider()
                    detail.shares.sortedBy { it.participantId }
                        .forEach { Text("${it.name}: ${money(it.amount)}") }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SharedExpenseForm(
    vm: SharedExpensesViewModel, contacts: List<ContactEntity>, accounts: List<AccountBalance>,
    onBack: () -> Unit, onContacts: () -> Unit
) {
    // ArrayLists de valores simples permiten restaurar el borrador al recrear la actividad.
    var id by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var title by rememberSaveable { mutableStateOf("") }
    var rows by rememberSaveable { mutableStateOf(arrayListOf("", "1", "")) }
    var selected by rememberSaveable { mutableStateOf(arrayListOf<Long>()) }
    var payer by rememberSaveable { mutableStateOf(0L) }
    var account by rememberSaveable { mutableStateOf<Long?>(null) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var amounts by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val busy = vm.saving || vm.saved
    val participants = listOf(0L) + selected
    val items = rows.chunked(3)
        .map { SharedItem(it[0], it[1].toLongOrNull() ?: 0, it[2].toLongOrNull() ?: 0) }
    val shares = amounts.associate {
        val parts = it.split(':'); parts[0].toLong() to (parts[1].toLongOrNull() ?: -1L)
    }
    val validation = runCatching {
        require(title.trim().length in 1..100) { "Escribe un título de hasta 100 caracteres." }
        require(selected.all { id -> contacts.any { it.id == id } }) { "Un contacto seleccionado ya no está activo." }
        require(payer != 0L || accounts.any { it.account.id == account }) { "Selecciona la cuenta desde la que pagaste." }
        if (custom) SharedExpensePlan.custom(
            items,
            participants,
            payer,
            participants.associateWith { shares[it] ?: -1L })
        else SharedExpensePlan.equal(items, participants, payer)
    }
    val plan = validation.getOrNull()
    fun name(participant: Long) =
        if (participant == 0L) "Yo" else contacts.firstOrNull { it.id == participant }?.name
            ?: "Contacto #$participant"
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack, enabled = !busy) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        "Volver"
                    )
                }
                QuestLabel("MISIÓN EN EQUIPO")
            }
            Text("Nuevo gasto compartido", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Registra una compra ya realizada. Usa pesos enteros, sin puntos ni comas.",
                color = SqTextSecondary
            )
        }
        item {
            OutlinedTextField(
                title,
                { title = it },
                Modifier.fillMaxWidth(),
                enabled = !busy,
                label = { Text("Título del gasto") },
                singleLine = true
            )
        }
        item { QuestLabel("PRODUCTOS O SERVICIOS") }
        items(rows.size / 3) { row ->
            QuestPanel {
                fun update(offset: Int, value: String) {
                    rows = ArrayList(rows).also { it[row * 3 + offset] = value }
                }
                OutlinedTextField(
                    rows[row * 3],
                    { update(0, it) },
                    Modifier.fillMaxWidth(),
                    enabled = !busy,
                    label = { Text("Producto ${row + 1}") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        rows[row * 3 + 1],
                        { update(1, it) },
                        Modifier.weight(1f),
                        enabled = !busy,
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        rows[row * 3 + 2],
                        { update(2, it) },
                        Modifier.weight(2f),
                        enabled = !busy,
                        label = { Text("Precio unitario") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                if (rows.size > 3) TextButton({
                    rows = ArrayList(rows.filterIndexed { index, _ -> index / 3 != row })
                }, enabled = !busy) { Text("Quitar producto ${row + 1}") }
            }
        }
        item {
            TextButton({ rows = ArrayList(rows + listOf("", "1", "")) }, enabled = !busy) {
                Text(
                    "Agregar producto"
                )
            }
        }
        item { QuestLabel("PARTICIPANTES"); Text("Tú siempre participas.") }
        if (contacts.isEmpty()) item {
            TextButton(
                onContacts,
                enabled = !busy
            ) { Text("Agrega un contacto para compartir") }
        }
        items(contacts, key = { "contact-${it.id}" }) { contact ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(contact.id in selected, { checked ->
                    selected =
                        ArrayList(if (checked) selected + contact.id else selected - contact.id)
                    if (!checked && payer == contact.id) payer = 0
                }, enabled = !busy)
                Text("${contact.name}${if (contact.phone.isNotBlank()) " · ${contact.phone}" else ""}")
            }
        }
        item { QuestLabel("QUIÉN PAGÓ") }
        items(participants, key = { "payer-$it" }) { participant ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(payer == participant, { payer = participant }, enabled = !busy)
                Text(name(participant))
            }
        }
        if (payer == 0L) {
            item { QuestLabel("CUENTA DE ORIGEN") }
            items(accounts, key = { "account-${it.account.id}" }) { balance ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        account == balance.account.id,
                        { account = balance.account.id },
                        enabled = !busy,
                        modifier = Modifier.testTag("pay-account-${balance.account.id}")
                    )
                    Text("${balance.account.name} · Disponible ${money(balance.available)}")
                }
            }
        }
        item {
            QuestLabel("REPARTO")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!custom, { custom = false }, label = { Text("Igual") }, enabled = !busy)
                FilterChip(
                    custom,
                    { custom = true },
                    label = { Text("Personalizado") },
                    enabled = !busy
                )
            }
        }
        if (custom) items(participants, key = { "share-$it" }) { participant ->
            val value = amounts.firstOrNull { it.startsWith("$participant:") }?.substringAfter(':')
                .orEmpty()
            OutlinedTextField(
                value,
                { amount ->
                    amounts =
                        ArrayList(amounts.filterNot { it.startsWith("$participant:") } + "$participant:$amount")
                },
                Modifier.fillMaxWidth(),
                enabled = !busy,
                label = { Text("Parte de ${name(participant)}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        item {
            if (plan != null) QuestPanel {
                QuestLabel("REVISA ANTES DE GUARDAR")
                Text("Total: ${money(plan.total)}")
                plan.shares.forEach { (participant, amount) ->
                    Text(
                        "${name(participant)}: ${
                            money(
                                amount
                            )
                        }"
                    )
                }
                Text("Tu gasto: ${money(plan.ownExpense)} · Sale de tu cuenta: ${money(plan.cashOutflow)}")
                Text(
                    "Por cobrar: ${money(plan.debts.sumOf { it.receivable })} · Por pagar: ${
                        money(
                            plan.debts.sumOf { it.payable })
                    }"
                )
                if (payer == 0L && plan.cashOutflow > (accounts.firstOrNull { it.account.id == account }?.available
                        ?: 0)
                )
                    Text(
                        "El pago supera el disponible registrado. Tu cuenta quedará por debajo del dinero reservado en metas.",
                        color = SqXp
                    )
            }
            if (submitted && validation.isFailure) Text(
                validation.exceptionOrNull()?.message.orEmpty(),
                color = SqExpense
            )
            vm.error?.let { Text(it, color = SqExpense) }
            vm.loadError?.let { Text(it, color = SqExpense) }
        }
        item {
            QuestButton(if (busy) "Guardando…" else "Guardar gasto compartido", {
                submitted = true
                if (plan != null) vm.save(
                    id,
                    title,
                    items,
                    participants,
                    payer,
                    account,
                    if (custom) plan.shares else null
                )
            }, enabled = !busy && vm.loadError == null)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
