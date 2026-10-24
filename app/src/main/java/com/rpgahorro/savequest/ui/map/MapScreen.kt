package com.rpgahorro.savequest.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rpgahorro.savequest.SaveQuestApplication
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.data.model.MapPlace
import com.rpgahorro.savequest.data.model.PlaceCategory
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.theme.*
import java.util.UUID
import kotlinx.coroutines.CancellationException

@Composable
fun MapScreen(
    transactions: List<TransactionEntity> = emptyList(),
    onRegisterExpense: (MapPlace) -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as SaveQuestApplication
    var catalog by remember { mutableStateOf<List<MapPlace>>(emptyList()) }
    var catalogError by remember { mutableStateOf(false) }
    var catalogLoading by remember { mutableStateOf(true) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(retry) {
        catalogLoading = true
        catalogError = false
        try {
            catalog = app.placeCatalog.load()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            catalogError = true
        } finally {
            catalogLoading = false
        }
    }
    val places =
        remember(catalog, transactions) {
            (catalog + transactions.mapNotNull { it.savedPlace() }).distinctBy { it.id }
        }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedPlaceJson by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPlace =
        remember(selectedPlaceJson) { selectedPlaceJson?.let { MapPlace.fromJson(it) } }
    var candidates by remember { mutableStateOf<List<Long>>(emptyList()) }
    var businessCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var street by rememberSaveable { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var manualLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var manualLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val visiblePlaces =
        remember(places, category, query) {
            places.filter {
                (category == null || it.category.key == category) &&
                        (query.isBlank() ||
                                it.name.contains(query.trim(), true) ||
                                it.address.contains(query.trim(), true))
            }
        }

    fun select(place: MapPlace) {
        selectedId = null
        selectedPlaceJson = place.toJson()
        query = ""
        keyboard?.hide()
    }

    val points =
        remember(transactions) {
            transactions.mapNotNull { row ->
                if (row.latitude == null || row.longitude == null) null
                else MapPoint(row.id, row.latitude, row.longitude, row.type == "EXPENSE")
            }
        }
    val selected = transactions.firstOrNull { it.id == selectedId }
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuestLabel("EXPLORA TU CAMINO")
        Text(
            "Comercios de Santiago",
            style = MaterialTheme.typography.titleLarge,
            color = SqTextPrimary,
        )
        OutlinedTextField(
            query,
            {
                query = it
                selectedPlaceJson = null
            },
            Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Buscar comercio o dirección") },
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                category == null,
                {
                    category = null
                    selectedPlaceJson = null
                },
                label = { Text("Todos") },
            )
            PlaceCategory.entries.forEach { item ->
                FilterChip(
                    category == item.key,
                    {
                        category = item.key
                        selectedPlaceJson = null
                    },
                    label = { Text(item.label) },
                )
            }
        }
        if (catalogLoading) Text("Cargando comercios…", style = MaterialTheme.typography.bodySmall)
        if (catalogError)
            TextButton({ retry++ }) { Text("No se cargaron los comercios. Reintentar") }
        if (query.isNotBlank()) {
            LazyColumn(Modifier.heightIn(max = 140.dp)) {
                if (visiblePlaces.isEmpty())
                    item {
                        Text(
                            "No se encontraron comercios. Puedes agregar un lugar con una pulsación larga en el mapa."
                        )
                    }
                items(visiblePlaces.take(8), key = { it.id }) { place ->
                    ListItem(
                        headlineContent = { Text(place.name) },
                        supportingContent = {
                            Text(place.address.ifBlank { place.category.label })
                        },
                        modifier =
                            Modifier
                                .testTag("place-result-${place.id}")
                                .clickable { select(place) },
                    )
                }
            }
        }
        Box(Modifier
            .weight(1f)
            .fillMaxWidth()) {
            OfflineMap(
                points,
                Modifier
                    .fillMaxSize()
                    .testTag("santiago-map"),
                places = visiblePlaces,
                focusedPlace = selectedPlace,
                initialZoom = 14.5,
                onPoint = {
                    selectedPlaceJson = null
                    if (it.size == 1) selectedId = it.first() else candidates = it
                },
                onBusiness = { ids ->
                    val matches = places.filter { it.id in ids }
                    if (matches.size == 1) select(matches.first()) else businessCandidates = ids
                },
                onLongPress = { lat, lon ->
                    manualLatitude = lat
                    manualLongitude = lon
                },
                onStreet = {
                    street = it
                    selectedId = null
                    selectedPlaceJson = null
                },
                onReady = { loaded = true; error = false },
                onError = { error = true },
            )
            if (!loaded || error)
                Text(
                    if (error) "No se pudo abrir el mapa local." else "Abriendo Santiago…",
                    color = SqTextPrimary,
                )
        }
        if (selectedPlace != null) {
            PlaceCard(selectedPlace, transactions, onRegisterExpense) { selectedPlaceJson = null }
        } else if (selected != null) {
            QuestPanel {
                Text(
                    selected.description.ifBlank { selected.placeName ?: selected.category },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${selected.category} · ${if (selected.type == "EXPENSE") "−" else "+"}${
                        money(
                            selected.amount
                        )
                    }",
                    color = if (selected.type == "EXPENSE") SqExpense else SqSavings,
                )
                selected.savedPlace()?.let { place ->
                    TextButton({ select(place) }) { Text("Ver ${place.name}") }
                }
            }
        } else
            Text(
                street.ifBlank {
                    "Acerca el mapa para ver nombres. Mantén pulsado para agregar un lugar."
                },
                style = MaterialTheme.typography.bodySmall,
                color = SqTextSecondary,
            )
        MapAttribution()
    }
    if (candidates.isNotEmpty())
        AlertDialog(
            onDismissRequest = { candidates = emptyList() },
            title = { Text("Movimientos en este lugar") },
            text = {
                LazyColumn {
                    items(transactions.filter { it.id in candidates }, key = { it.id }) { row ->
                        ListItem(
                            headlineContent = {
                                Text(row.description.ifBlank { row.placeName ?: row.category })
                            },
                            supportingContent = { Text("${row.category} · ${money(row.amount)}") },
                            modifier =
                                Modifier.clickable {
                                    selectedId = row.id
                                    candidates = emptyList()
                                },
                        )
                    }
                }
            },
            confirmButton = { TextButton({ candidates = emptyList() }) { Text("Cerrar") } },
        )
    if (businessCandidates.isNotEmpty())
        AlertDialog(
            onDismissRequest = { businessCandidates = emptyList() },
            title = { Text("Comercios cercanos") },
            text = {
                LazyColumn {
                    items(places.filter { it.id in businessCandidates }, key = { it.id }) { place ->
                        ListItem(
                            headlineContent = { Text(place.name) },
                            supportingContent = { Text(place.address) },
                            modifier =
                                Modifier.clickable {
                                    select(place)
                                    businessCandidates = emptyList()
                                },
                        )
                    }
                }
            },
            confirmButton = { TextButton({ businessCandidates = emptyList() }) { Text("Cerrar") } },
        )
    if (manualLatitude != null && manualLongitude != null) {
        var name by rememberSaveable { mutableStateOf("") }
        var kind by rememberSaveable { mutableStateOf(PlaceCategory.SHOPPING.key) }
        AlertDialog(
            onDismissRequest = {
                manualLatitude = null
                manualLongitude = null
            },
            title = { Text("Agregar lugar") },
            text = {
                Column {
                    OutlinedTextField(
                        name,
                        { name = it },
                        label = { Text("Nombre del lugar") },
                        singleLine = true,
                    )
                    PlaceCategory.entries.forEach { item ->
                        FilterChip(
                            kind == item.key,
                            { kind = item.key },
                            label = { Text(item.label) },
                        )
                    }
                    Text(
                        "El lugar quedará guardado junto con tu gasto.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    {
                        select(
                            MapPlace(
                                "local:${UUID.randomUUID()}",
                                name.trim(),
                                PlaceCategory.fromKey(kind),
                                "",
                                manualLatitude!!,
                                manualLongitude!!,
                            )
                        )
                        manualLatitude = null
                        manualLongitude = null
                    },
                    enabled = name.trim().length in 1..200,
                ) {
                    Text("Usar lugar")
                }
            },
            dismissButton = {
                TextButton({
                    manualLatitude = null
                    manualLongitude = null
                }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun PlaceCard(
    place: MapPlace,
    transactions: List<TransactionEntity>,
    onRegister: (MapPlace) -> Unit,
    onClose: () -> Unit,
) {
    val purchases =
        remember(place.id, transactions) {
            transactions.filter { it.type == "EXPENSE" && it.placeId == place.id }
        }
    var history by rememberSaveable(place.id) { mutableStateOf(false) }
    QuestPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(place.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClose) { Text("Cerrar") }
        }
        Text(
            place.address.ifBlank { "Dirección no disponible · ${place.category.label}" },
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton({ history = true }, enabled = purchases.isNotEmpty()) {
            Text("${purchases.size} compras · ${money(purchases.sumOf { it.amount })} en total")
        }
        QuestButton("Registrar gasto aquí", { onRegister(place) })
    }
    if (history)
        AlertDialog(
            onDismissRequest = { history = false },
            title = { Text("Compras en ${place.name}") },
            text = {
                LazyColumn {
                    items(purchases, key = { it.id }) { purchase ->
                        ListItem(
                            headlineContent = {
                                Text("${money(purchase.amount)} · ${purchase.category}")
                            },
                            supportingContent = {
                                Text(
                                    java.text.DateFormat.getDateInstance(
                                        java.text.DateFormat.SHORT,
                                        java.util.Locale.forLanguageTag("es-CL"),
                                    )
                                        .format(java.util.Date(purchase.date)) +
                                            " · " +
                                            purchase.description
                                )
                            },
                        )
                    }
                }
            },
            confirmButton = { TextButton({ history = false }) { Text("Cerrar") } },
        )
}

@Composable
fun MapAttribution() {
    val uri = LocalUriHandler.current
    Column {
        Text(
            "Mapa disponible sin conexión · Zona urbana de Santiago · Datos de comercios pueden variar.",
            style = MaterialTheme.typography.labelSmall,
            color = SqTextSecondary,
        )
        TextButton(
            onClick = { uri.openUri("https://www.openstreetmap.org/copyright") },
            contentPadding = PaddingValues(0.dp),
        ) {
            Text("© OpenStreetMap contributors · ODbL", style = MaterialTheme.typography.labelSmall)
        }
    }
}
