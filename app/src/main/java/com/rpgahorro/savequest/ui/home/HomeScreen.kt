package com.rpgahorro.savequest.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.theme.*
import java.time.LocalTime

@Composable
fun HomeScreen(uiState: HomeUiState, onGoalsClick: () -> Unit = {}, onContribute: (Long) -> Unit = {}) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuestIllustration(QuestArt.Crystal, Modifier.size(36.dp))
                Text("SAVEQUEST", color = SqPrimarySoft, fontSize = 18.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Text(when (LocalTime.now().hour) { in 6..11 -> "Buenos días"; in 12..19 -> "Buenas tardes"; else -> "Buenas noches" }, color = SqTextSecondary)
            Text("Tu aventura de ahorro", style = MaterialTheme.typography.headlineLarge, color = SqTextPrimary)
        }
        item { XpProgressCard(uiState.level, uiState.currentLevelXp, uiState.xpForNextLevel) }
        item {
            QuestPanel {
                QuestLabel("PATRIMONIO TOTAL")
                Text(money(uiState.balance), color = SqTextPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FinancialValue("Disponible", money(uiState.available), SqSavings, Modifier.weight(1f))
                    FinancialValue("Reservado en metas", money(uiState.reserved), SqPrimarySoft, Modifier.weight(1f))
                }
                HorizontalDivider(color = SqSurfaceElevated)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FinancialValue("↑ Ingresos", "+${money(uiState.totalIncome)}", SqSavings, Modifier.weight(1f))
                    FinancialValue("↓ Gastos", "−${money(uiState.totalExpenses)}", SqExpense, Modifier.weight(1f))
                }
            }
        }
        item {
            val goal = uiState.goals.firstOrNull { it.savedAmount < it.goal.targetAmount } ?: uiState.goals.firstOrNull()
            if (goal == null) {
                QuestPanel {
                    QuestLabel("TU PRIMERA META")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuestIllustration(QuestArt.Treasure, Modifier.size(64.dp))
                        Text("Dale un propósito a tu ahorro.", color = SqTextPrimary, modifier = Modifier.weight(1f))
                    }
                    QuestButton("+  Crear una meta", onGoalsClick)
                }
            } else SavingGoalCard(goal.goal.name, goal.savedAmount, goal.goal.targetAmount, { onContribute(goal.goal.id) })
        }
        item { QuestLabel("DESAFÍOS") }
        items(uiState.challenges, key = { "challenge-${it.id}" }) {
            ChallengeCard(it.title, it.description, it.progress, it.target, it.xpReward, it.completed)
        }
        item { QuestLabel("MOVIMIENTOS RECIENTES") }
        if (uiState.recentTransactions.isEmpty()) item {
            Text("Registra tu primer ingreso para comenzar la aventura.", color = SqTextSecondary)
        }
        items(uiState.recentTransactions, key = { "transaction-${it.id}" }) {
            TransactionItem(it.description.ifBlank { it.category }, it.category, it.amount, it.type == TransactionType.INCOME.name)
        }
    }
}

@Composable
private fun FinancialValue(label: String, amount: String, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = SqTextSecondary, fontSize = 12.sp)
        Text(amount, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge)
    }
}
