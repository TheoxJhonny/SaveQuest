package com.rpgahorro.savequest

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rpgahorro.savequest.navigation.SaveQuestDestination
import com.rpgahorro.savequest.ui.components.SaveQuestBottomBar
import com.rpgahorro.savequest.ui.goals.GoalsScreen
import com.rpgahorro.savequest.ui.home.HomeScreen
import com.rpgahorro.savequest.ui.map.MapScreen
import com.rpgahorro.savequest.ui.profile.ProfileScreen
import com.rpgahorro.savequest.ui.transactions.TransactionsScreen
import com.rpgahorro.savequest.ui.theme.SqBackground
import com.rpgahorro.savequest.ui.theme.SqPrimary

@Composable
fun SaveQuestApp() {

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry
        ?.destination
        ?.route

    Scaffold(
        containerColor = SqBackground,

        bottomBar = {

            SaveQuestBottomBar(
                currentRoute = currentRoute,

                onNavigate = { destination ->

                    navController.navigate(destination.route) {

                        popUpTo(SaveQuestDestination.Home.route) {
                            saveState = true
                        }

                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },

        floatingActionButton = {

            FloatingActionButton(
                onClick = {
                    // Lo implementaremos después.
                },
                containerColor = SqPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {

                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Registrar movimiento"
                )
            }
        }

    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = SaveQuestDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
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
}