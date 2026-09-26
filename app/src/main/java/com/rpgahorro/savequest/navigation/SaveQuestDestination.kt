package com.rpgahorro.savequest.navigation

sealed class SaveQuestDestination(
    val route: String,
    val label: String
) {

    data object Home : SaveQuestDestination(
        route = "home",
        label = "Inicio"
    )

    data object Transactions : SaveQuestDestination(
        route = "transactions",
        label = "Movimientos"
    )

    data object Goals : SaveQuestDestination(
        route = "goals",
        label = "Metas"
    )

    data object Map : SaveQuestDestination(
        route = "map",
        label = "Mapa"
    )

    data object Profile : SaveQuestDestination(
        route = "profile",
        label = "Perfil"
    )
}