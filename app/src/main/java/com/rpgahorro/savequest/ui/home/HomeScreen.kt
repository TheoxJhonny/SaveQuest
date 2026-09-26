package com.rpgahorro.savequest.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpgahorro.savequest.ui.components.SavingGoalCard
import com.rpgahorro.savequest.ui.components.XpProgressCard
import com.rpgahorro.savequest.ui.theme.SqPrimary
import com.rpgahorro.savequest.ui.theme.SqTextSecondary
import com.rpgahorro.savequest.ui.components.ChallengeCard
import com.rpgahorro.savequest.ui.components.TransactionItem

@Composable
fun HomeScreen() {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 24.dp,
            end = 24.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        item {

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SAVEQUEST",
                color = SqPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Buenas noches",
                color = SqTextSecondary,
                fontSize = 15.sp
            )

            Text(
                text = "Tu progreso",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            XpProgressCard(
                level = 5,
                currentXp = 820,
                requiredXp = 1000
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "PATRIMONIO TOTAL",
                color = SqTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$705.000",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(36.dp))

            SavingGoalCard(
                name = "Notebook",
                currentAmount = 315000,
                targetAmount = 900000
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "DESAFÍOS",
                color = SqTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            ChallengeCard(
                title = "Mantén el ritmo",
                description = "Registra 5 movimientos esta semana.",
                currentProgress = 3,
                targetProgress = 5,
                xpReward = 100
            )

            Spacer(modifier = Modifier.height(12.dp))

            ChallengeCard(
                title = "Pequeños pasos",
                description = "Realiza 2 aportes a una meta.",
                currentProgress = 1,
                targetProgress = 2,
                xpReward = 75
            )

            Spacer(modifier = Modifier.height(36.dp))


            Text(
                text = "MOVIMIENTOS RECIENTES",
                color = SqTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            TransactionItem(
                title = "Supermercado",
                category = "Alimentación",
                amount = 28990,
                isIncome = false
            )

            TransactionItem(
                title = "Aporte Notebook",
                category = "Ahorro",
                amount = 30000,
                isIncome = true
            )

            TransactionItem(
                title = "Café",
                category = "Alimentación",
                amount = 3500,
                isIncome = false
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}