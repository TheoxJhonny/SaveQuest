package com.rpgahorro.savequest.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class SaveQuestDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {

    data object Home : SaveQuestDestination(
        route = "home",
        label = "Inicio",
        icon = Icons.Outlined.Home
    )

    data object Transactions : SaveQuestDestination(
        route = "transactions",
        label = "Movimientos",
        icon = Icons.Outlined.AccountBalanceWallet
    )

    data object Goals : SaveQuestDestination(
        route = "goals",
        label = "Metas",
        icon = Icons.Outlined.Flag
    )

    data object Map : SaveQuestDestination(
        route = "map",
        label = "Mapa",
        icon = Icons.Outlined.Map
    )

    data object Profile : SaveQuestDestination(
        route = "profile",
        label = "Perfil",
        icon = Icons.Outlined.Person
    )
}

val bottomDestinations = listOf(
    SaveQuestDestination.Home,
    SaveQuestDestination.Transactions,
    SaveQuestDestination.Goals,
    SaveQuestDestination.Map,
    SaveQuestDestination.Profile
)