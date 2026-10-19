package com.rpgahorro.savequest.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.home.HomeUiState
import com.rpgahorro.savequest.ui.theme.*

@Composable
fun ProfileScreen(uiState: HomeUiState, onAccounts: () -> Unit, onContacts: () -> Unit, onSharedExpenses: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        QuestLabel("TU AVENTURA")
        Text("Perfil", style = MaterialTheme.typography.headlineLarge, color = SqTextPrimary)
        XpProgressCard(uiState.level, uiState.currentLevelXp, uiState.xpForNextLevel)
        QuestPanel {
            Text("Tu progreso", style = MaterialTheme.typography.titleLarge)
            Text("${uiState.totalXp} XP acumulados", color = SqXp)
            Text("${uiState.goals.count { it.savedAmount >= it.goal.targetAmount }} metas completadas", color = SqSavings)
            Text("${uiState.challenges.count { it.completed }} desafíos completados", color = SqTextSecondary)
        }
        QuestButton("Mis cuentas", onAccounts)
        QuestButton("Contactos", onContacts)
        QuestButton("Gastos compartidos", onSharedExpenses)
        Spacer(Modifier.height(80.dp))
    }
}
