package com.rpgahorro.savequest

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.navigation.SaveQuestDestination
import com.rpgahorro.savequest.ui.accounts.AccountsScreen
import com.rpgahorro.savequest.ui.accounts.AccountsViewModel
import com.rpgahorro.savequest.ui.components.AddMovementSheet
import com.rpgahorro.savequest.ui.components.SaveQuestBottomBar
import com.rpgahorro.savequest.ui.goals.GoalsScreen
import com.rpgahorro.savequest.ui.goals.GoalsViewModel
import com.rpgahorro.savequest.ui.home.HomeScreen
import com.rpgahorro.savequest.ui.home.HomeViewModel
import com.rpgahorro.savequest.ui.home.HomeViewModelFactory
import com.rpgahorro.savequest.ui.map.MapScreen
import com.rpgahorro.savequest.ui.profile.ProfileScreen
import com.rpgahorro.savequest.ui.theme.SqBackground
import com.rpgahorro.savequest.ui.theme.SqPrimary
import com.rpgahorro.savequest.ui.transactions.AddTransactionScreen
import com.rpgahorro.savequest.ui.transactions.TransactionViewModel
import com.rpgahorro.savequest.ui.transactions.TransactionViewModelFactory
import com.rpgahorro.savequest.ui.transactions.TransactionsScreen

@Composable
fun SaveQuestApp(
    phoneContacts: com.rpgahorro.savequest.ui.contacts.PhoneContactPickerState? = null
) {

    val navController = rememberNavController()
    androidx.compose.runtime.LaunchedEffect(phoneContacts?.draft, phoneContacts?.error) {
        if (phoneContacts?.draft != null || phoneContacts?.error != null) {
            navController.navigate(SaveQuestDestination.Contacts.route) { launchSingleTop = true }
        }
    }

    var showAddMovementSheet by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route

    val showMainNavigation =
        currentRoute in
            listOf(
                SaveQuestDestination.Home.route,
                SaveQuestDestination.Transactions.route,
                SaveQuestDestination.Goals.route,
                SaveQuestDestination.Map.route,
                SaveQuestDestination.Profile.route,
            )

    val context = LocalContext.current

    val application = context.applicationContext as SaveQuestApplication

    val transactionViewModel: TransactionViewModel =
        viewModel(factory = TransactionViewModelFactory(application.transactionRepository))

    val goalsViewModel: GoalsViewModel =
        viewModel(
            factory =
                GoalsViewModel.Factory(
                    application.savingGoalRepository,
                    application.accountRepository,
                )
        )
    val accountsViewModel: AccountsViewModel =
        viewModel(factory = AccountsViewModel.Factory(application.accountRepository))
    val accounts by accountsViewModel.accounts.collectAsState()
    var pendingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }

    val homeViewModel: HomeViewModel =
        viewModel(
            factory =
                HomeViewModelFactory(
                    application.transactionRepository,
                    application.userProgressRepository,
                    application.challengeRepository,
                    application.savingGoalRepository,
                    application.sharedExpenseRepository,
                )
        )

    val homeUiState by homeViewModel.uiState.collectAsState()

    val transactions by transactionViewModel.transactions.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = SqBackground,
        bottomBar = {
            if (showMainNavigation) {

                SaveQuestBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(SaveQuestDestination.Home.route) { saveState = true }

                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (showMainNavigation && currentRoute != SaveQuestDestination.Map.route) {

                FloatingActionButton(
                    onClick = { showAddMovementSheet = true },
                    containerColor = SqPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Registrar movimiento",
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SaveQuestDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(SaveQuestDestination.Home.route) {
                HomeScreen(
                    uiState = homeUiState,
                    onGoalsClick = {
                        navController.navigate(SaveQuestDestination.Goals.route) {
                            launchSingleTop = true
                        }
                    },
                    onContribute = { id ->
                        pendingGoalId = id
                        navController.navigate(SaveQuestDestination.Goals.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(SaveQuestDestination.Transactions.route) {
                TransactionsScreen(transactions = transactions, accounts = accounts)
            }

            composable(SaveQuestDestination.Goals.route) {
                GoalsScreen(
                    goalsViewModel,
                    homeUiState.available,
                    pendingGoalId,
                    { pendingGoalId = null },
                )
            }

            composable(SaveQuestDestination.Map.route) {
                MapScreen(transactions) { place ->
                    transactionViewModel.clearStatus()
                    navController.navigate(
                        SaveQuestDestination.AddExpense.route +
                            "?place=" +
                            android.net.Uri.encode(place.toJson())
                    )
                }
            }

            composable(SaveQuestDestination.Profile.route) {
                ProfileScreen(
                    homeUiState,
                    {
                        navController.navigate(SaveQuestDestination.Accounts.route) {
                            launchSingleTop = true
                        }
                    },
                    {
                        navController.navigate(SaveQuestDestination.Contacts.route) {
                            launchSingleTop = true
                        }
                    },
                    {
                        navController.navigate(SaveQuestDestination.SharedExpenses.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(SaveQuestDestination.SharedExpenses.route) {
                val vm: com.rpgahorro.savequest.ui.shared.SharedExpensesViewModel =
                    viewModel(
                        factory =
                            com.rpgahorro.savequest.ui.shared.SharedExpensesViewModel.Factory(
                                application.sharedExpenseRepository,
                                application.contactRepository,
                                application.accountRepository,
                            )
                    )
                com.rpgahorro.savequest.ui.shared.SharedExpensesScreen(
                    vm,
                    { navController.popBackStack() },
                    { navController.navigate(SaveQuestDestination.Contacts.route) },
                )
            }

            composable(SaveQuestDestination.Contacts.route) {
                val contactsViewModel: com.rpgahorro.savequest.ui.contacts.ContactsViewModel =
                    viewModel(
                        factory =
                            com.rpgahorro.savequest.ui.contacts.ContactsViewModel.Factory(
                                application.contactRepository
                            )
                    )
                com.rpgahorro.savequest.ui.contacts.ContactsScreen(
                    contactsViewModel,
                    { navController.popBackStack() },
                    phoneContacts = phoneContacts,
                )
            }

            composable(SaveQuestDestination.Accounts.route) {
                AccountsScreen(accountsViewModel, { navController.popBackStack() })
            }

            listOf(
                    SaveQuestDestination.AddExpense.route to TransactionType.EXPENSE,
                    SaveQuestDestination.AddIncome.route to TransactionType.INCOME,
                )
                .forEach { (route, type) ->
                    composable(
                        route + "?place={place}",
                        arguments =
                            listOf(androidx.navigation.navArgument("place") { defaultValue = "" }),
                    ) { entry ->
                        val placeJson = entry.arguments?.getString("place").orEmpty()
                        val initialPlace =
                            remember(placeJson) {
                                if (type == TransactionType.EXPENSE && placeJson.isNotBlank())
                                    com.rpgahorro.savequest.data.model.MapPlace.fromJson(placeJson)
                                else null
                            }
                        AddTransactionScreen(
                            type = type,
                            initialPlace = initialPlace,
                            saveState = transactionViewModel.saveState,
                            accounts = accounts,
                            onBack = {
                                transactionViewModel.clearStatus()
                                navController.popBackStack()
                            },
                            onSaved = {
                                transactionViewModel.clearStatus()
                                navController.popBackStack()
                            },
                            onSave = {
                                amount,
                                category,
                                description,
                                accountId,
                                latitude,
                                longitude,
                                place ->
                                transactionViewModel.addTransaction(
                                    type,
                                    amount,
                                    category,
                                    description,
                                    accountId,
                                    latitude,
                                    longitude,
                                    place,
                                )
                            },
                        )
                    }
                }
        }
    }

    if (showAddMovementSheet) {

        AddMovementSheet(
            onDismiss = { showAddMovementSheet = false },
            onExpenseClick = {
                transactionViewModel.clearStatus()
                showAddMovementSheet = false

                navController.navigate(SaveQuestDestination.AddExpense.route)
            },
            onIncomeClick = {
                transactionViewModel.clearStatus()
                showAddMovementSheet = false

                navController.navigate(SaveQuestDestination.AddIncome.route)
            },
            onSharedExpenseClick = {
                showAddMovementSheet = false
                navController.navigate(SaveQuestDestination.SharedExpenses.route) {
                    launchSingleTop = true
                }
            },
            onGoalContributionClick = {
                showAddMovementSheet = false
                navController.navigate(SaveQuestDestination.Goals.route) { launchSingleTop = true }
            },
        )
    }
}
