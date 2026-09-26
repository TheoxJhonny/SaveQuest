package com.rpgahorro.savequest

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rpgahorro.savequest.navigation.SaveQuestDestination
import com.rpgahorro.savequest.ui.goals.GoalsScreen
import com.rpgahorro.savequest.ui.home.HomeScreen
import com.rpgahorro.savequest.ui.map.MapScreen
import com.rpgahorro.savequest.ui.profile.ProfileScreen
import com.rpgahorro.savequest.ui.transactions.TransactionsScreen

@Composable
fun SaveQuestApp() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SaveQuestDestination.Home.route
    ) {

        composable(SaveQuestDestination.Home.route) {
            HomeScreen()
        }

        composable(SaveQuestDestination.Transactions.route) {
            TransactionsScreen()
        }

        composable(SaveQuestDestination.Goals.route) {
            GoalsScreen()
        }

        composable(SaveQuestDestination.Map.route) {
            MapScreen()
        }

        composable(SaveQuestDestination.Profile.route) {
            ProfileScreen()
        }
    }
}