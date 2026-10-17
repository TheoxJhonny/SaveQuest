package com.rpgahorro.savequest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.rpgahorro.savequest.ui.theme.*

@Composable
fun SavingGoalCard(name: String, currentAmount: Long, targetAmount: Long,
                   onContribute: () -> Unit, heading: String = "META PRINCIPAL") {
    val progress = if (targetAmount > 0) (currentAmount.toFloat() / targetAmount).coerceIn(0f, 1f) else 0f
    QuestPanel {
        QuestLabel(heading)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuestIllustration(QuestArt.Treasure, Modifier.size(68.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, Modifier.weight(1f), color = SqTextPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text("${(progress * 100).toInt()}%", color = SqSavings, fontWeight = FontWeight.Bold)
                }
                QuestProgress(progress, SqSavings)
                Text("${money(currentAmount)} de ${money(targetAmount)}", color = SqTextSecondary, fontSize = 12.sp)
            }
        }
        QuestButton(if (progress >= 1f) "Meta completada" else "+  Aportar a meta", onContribute, progress < 1f)
    }
}
