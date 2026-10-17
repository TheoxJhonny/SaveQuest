package com.rpgahorro.savequest.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Castle
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.RemoveCircle

sealed class SaveQuestDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {

    data object Home : SaveQuestDestination(
        route = "home",
        label = "Inicio",
        icon = Icons.Outlined.Castle
    )

    data object Transactions : SaveQuestDestination(
        route = "transactions",
        label = "Movimientos",
        icon = Icons.AutoMirrored.Outlined.MenuBook
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
        icon = Icons.Outlined.Shield
    )

    data object AddExpense : SaveQuestDestination(
        route = "add_expense",
        label = "Registrar gasto",
        icon = Icons.Outlined.RemoveCircle
    )

    data object AddIncome : SaveQuestDestination(
        route = "add_income",
        label = "Registrar ingreso",
        icon = Icons.Outlined.AddCircle
    )
}

val bottomDestinations = listOf(
    SaveQuestDestination.Home,
    SaveQuestDestination.Transactions,
    SaveQuestDestination.Goals,
    SaveQuestDestination.Map,
    SaveQuestDestination.Profile
)