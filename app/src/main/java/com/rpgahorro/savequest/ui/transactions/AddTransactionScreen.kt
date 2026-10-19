package com.rpgahorro.savequest.ui.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rpgahorro.savequest.data.local.entity.AccountBalance
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.ui.accounts.AccountPicker
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.theme.*

@Composable
fun AddTransactionScreen(
    type: TransactionType,
    onBack: () -> Unit,
    onSave:
        (
        Long,
        String,
        String,
        Long,
        Double?,
        Double?,
        com.rpgahorro.savequest.data.model.MapPlace?,
    ) -> Unit,
    accounts: List<AccountBalance>,
    saveState: TransactionSaveState = TransactionSaveState(),
    onSaved: () -> Unit = {},
    initialPlace: com.rpgahorro.savequest.data.model.MapPlace? = null,
) {
    var latitude by rememberSaveable(type) { mutableStateOf<Double?>(initialPlace?.latitude) }
    var longitude by rememberSaveable(type) { mutableStateOf<Double?>(initialPlace?.longitude) }
    var placeJson by rememberSaveable(type) { mutableStateOf(initialPlace?.toJson()) }
    val place =
        remember(placeJson) {
            placeJson?.let { com.rpgahorro.savequest.data.model.MapPlace.fromJson(it) }
        }
    var accountId by rememberSaveable(type) { mutableLongStateOf(1L) }
    var amount by rememberSaveable(type) { mutableStateOf("") }
    var category by
    rememberSaveable(type) { mutableStateOf(initialPlace?.category?.expenseCategory.orEmpty()) }
    var description by rememberSaveable(type) { mutableStateOf("") }
    var submitted by rememberSaveable(type) { mutableStateOf(false) }
    val parsed = amount.toLongOrNull()
    val invalidAmount =
        amount.isEmpty() || amount.any { it !in '0'..'9' } || parsed == null || parsed <= 0
    val busy = saveState.saving || saveState.saved
    val expense = type == TransactionType.EXPENSE
    val accent = if (expense) SqExpense else SqSavings

    LaunchedEffect(saveState.saved) { if (saveState.saved) onSaved() }
    BackHandler(enabled = busy) { /* Esperar confirmación antes de abandonar el formulario. */ }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, enabled = !busy) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver")
            }
            QuestLabel("REGISTRA TU AVANCE")
        }
        Text(
            if (expense) "Registrar gasto" else "Registrar ingreso",
            style = MaterialTheme.typography.headlineLarge,
            color = SqTextPrimary,
        )
        QuestPanel {
            Icon(
                if (expense) Icons.Outlined.RemoveCircle else Icons.Outlined.AddCircle,
                null,
                Modifier.size(36.dp),
                tint = accent,
            )
            Text(
                if (expense) "¿En qué utilizaste tu dinero?"
                else "Registra el dinero que recibiste.",
                color = SqTextSecondary,
            )
            if (place != null) {
                Text(place.name, style = MaterialTheme.typography.titleMedium)
                if (place.address.isNotBlank()) Text(place.address, color = SqTextSecondary)
                TextButton({ placeJson = null }, enabled = !busy) { Text("Quitar comercio") }
            }
            AccountPicker(accounts, accountId, !busy) { accountId = it }
            OutlinedTextField(
                amount,
                { amount = it },
                Modifier.fillMaxWidth(),
                enabled = !busy,
                label = { Text("Monto en pesos") },
                prefix = { Text("$") },
                singleLine = true,
                isError = submitted && invalidAmount,
                supportingText = {
                    Text(
                        if (submitted && invalidAmount) "Ingresa un monto válido mayor que cero."
                        else "Sin puntos ni comas"
                    )
                },
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                category,
                { category = it },
                Modifier.fillMaxWidth(),
                enabled = !busy,
                label = { Text("Categoría") },
                singleLine = true,
                isError = submitted && category.isBlank(),
                supportingText = {
                    if (submitted && category.isBlank()) Text("Escribe una categoría.")
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                description,
                { description = it },
                Modifier.fillMaxWidth(),
                enabled = !busy,
                label = { Text("Descripción (opcional)") },
                minLines = 2,
                maxLines = 4,
            )
            com.rpgahorro.savequest.ui.map.MovementLocation(latitude, longitude, !busy) { lat, lon
                ->
                latitude = lat
                longitude = lon
                if (place != null && (place.latitude != lat || place.longitude != lon))
                    placeJson = null
            }
            saveState.error?.let { Text(it, color = SqExpense) }
            QuestButton(
                if (busy) "Guardando…" else "Guardar movimiento",
                {
                    submitted = true
                    if (!invalidAmount && category.isNotBlank())
                        onSave(
                            parsed!!,
                            category.trim(),
                            description.trim(),
                            accountId,
                            latitude,
                            longitude,
                            place,
                        )
                },
                enabled = !busy && accounts.any { it.account.id == accountId },
            )
        }
        Text("★ +10 XP por registrar tu movimiento", color = SqXp)
        Spacer(Modifier.height(16.dp))
    }
}
