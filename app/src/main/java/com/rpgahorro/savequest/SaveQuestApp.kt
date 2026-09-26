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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rpgahorro.savequest.ui.components.AddMovementSheet
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.ui.transactions.AddTransactionScreen
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rpgahorro.savequest.ui.transactions.TransactionViewModel
import com.rpgahorro.savequest.ui.transactions.TransactionViewModelFactory
import androidx.compose.runtime.collectAsState
import com.rpgahorro.savequest.ui.home.HomeViewModel
import com.rpgahorro.savequest.ui.home.HomeViewModelFactory

@Composable
fun SaveQuestApp() {

    val navController = rememberNavController()

    var showAddMovementSheet by remember {
        mutableStateOf(false)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry
        ?.destination
        ?.route

    val showMainNavigation = currentRoute in listOf(
        SaveQuestDestination.Home.route,
        SaveQuestDestination.Transactions.route,
        SaveQuestDestination.Goals.route,
        SaveQuestDestination.Map.route,
        SaveQuestDestination.Profile.route
    )

    val context = LocalContext.current

    val application =
        context.applicationContext as SaveQuestApplication

    val transactionViewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModelFactory(
            application.transactionRepository,
            application.userProgressRepository
        )
    )

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            application.transactionRepository,
            application.userProgressRepository
        )
    )

    val homeUiState by homeViewModel.uiState.collectAsState()

    val transactions by transactionViewModel.transactions.collectAsState(
        initial = emptyList()
    )

    Scaffold(
        containerColor = SqBackground,


        bottomBar = {

            if (showMainNavigation) {

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
            }
        },

        floatingActionButton = {

            if (showMainNavigation) {

                FloatingActionButton(
                    onClick = {
                        showAddMovementSheet = true
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
        }



    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = SaveQuestDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(SaveQuestDestination.Home.route) {

                HomeScreen(
                    uiState = homeUiState
                )
            }

            composable(SaveQuestDestination.Transactions.route) {

                TransactionsScreen(
                    transactions = transactions
                )
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

            composable(SaveQuestDestination.AddExpense.route) {

                AddTransactionScreen(
                    type = TransactionType.EXPENSE,

                    onBack = {
                        navController.popBackStack()
                    },

                    onSave = { amount, category, description ->

                        transactionViewModel.addTransaction(
                            type = TransactionType.EXPENSE,
                            amount = amount,
                            category = category,
                            description = description
                        )

                        navController.popBackStack()
                    }
                )
            }

            composable(SaveQuestDestination.AddIncome.route) {

                AddTransactionScreen(
                    type = TransactionType.INCOME,

                    onBack = {
                        navController.popBackStack()
                    },

                    onSave = { amount, category, description ->

                        transactionViewModel.addTransaction(
                            type = TransactionType.INCOME,
                            amount = amount,
                            category = category,
                            description = description
                        )

                        navController.popBackStack()
                    }
                )
            }
        }
    }

    if (showAddMovementSheet) {

        AddMovementSheet(

            onDismiss = {
                showAddMovementSheet = false
            },

            onExpenseClick = {
                showAddMovementSheet = false

                navController.navigate(
                    SaveQuestDestination.AddExpense.route
                )
            },

            onIncomeClick = {
                showAddMovementSheet = false

                navController.navigate(
                    SaveQuestDestination.AddIncome.route
                )
            },
            onGoalContributionClick = {
                showAddMovementSheet = false
            }
        )
    }
}