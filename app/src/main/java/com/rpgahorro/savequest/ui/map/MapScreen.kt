package com.rpgahorro.savequest.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.theme.*

@Composable
fun MapScreen(transactions: List<TransactionEntity> = emptyList()) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var candidates by remember { mutableStateOf<List<Long>>(emptyList()) }
    var street by rememberSaveable { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val points = remember(transactions) { transactions.mapNotNull { row ->
        if (row.latitude == null || row.longitude == null) null
        else MapPoint(row.id, row.latitude, row.longitude, row.type == "EXPENSE")
    } }
    val selected = transactions.firstOrNull { it.id == selectedId }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QuestLabel("EXPLORA TU CAMINO")
        Text("Santiago", style = MaterialTheme.typography.headlineLarge, color = SqTextPrimary)
        Text("Mapa sin conexión · ${points.size} ${if (points.size == 1) "movimiento" else "movimientos"} con ubicación", color = SqTextSecondary)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            OfflineMap(points, Modifier.fillMaxSize().testTag("santiago-map"), onPoint = {
                if (it.size == 1) selectedId = it.first() else candidates = it
            },
                onStreet = { street = it; selectedId = null },
                onReady = { loaded = true }, onError = { error = true })
            if (!loaded || error) Text(if (error) "No se pudo abrir el mapa local." else "Abriendo Santiago…", color = SqTextPrimary)
        }
        if (selected != null) QuestPanel {
            Text(selected.description.ifBlank { selected.category }, style = MaterialTheme.typography.titleMedium)
            Text("${selected.category} · ${if (selected.type == "EXPENSE") "−" else "+"}${money(selected.amount)}",
                color = if (selected.type == "EXPENSE") SqExpense else SqSavings)
        } else Text(street.ifBlank { "Toca una calle o un movimiento para ver su detalle." }, color = SqTextSecondary)
        MapAttribution()
    }
    if (candidates.isNotEmpty()) AlertDialog(onDismissRequest = { candidates = emptyList() },
        title = { Text("Movimientos en este lugar") },
        text = {
            LazyColumn {
                items(transactions.filter { it.id in candidates }, key = { it.id }) { row ->
                    ListItem(headlineContent = { Text(row.description.ifBlank { row.category }) },
                        supportingContent = { Text("${row.category} · ${money(row.amount)}") },
                        modifier = Modifier.clickable { selectedId = row.id; candidates = emptyList() })
                }
            }
        }, confirmButton = { TextButton({ candidates = emptyList() }) { Text("Cerrar") } })
}

@Composable
fun MapAttribution() {
    val uri = LocalUriHandler.current
    Column {
        Text("Cobertura: zona urbana de Santiago. No incluye rutas ni navegación.", style = MaterialTheme.typography.labelSmall, color = SqTextSecondary)
        TextButton(onClick = { uri.openUri("https://www.openstreetmap.org/copyright") }, contentPadding = PaddingValues(0.dp)) {
            Text("© OpenStreetMap contributors · ODbL", style = MaterialTheme.typography.labelSmall)
        }
    }
}
