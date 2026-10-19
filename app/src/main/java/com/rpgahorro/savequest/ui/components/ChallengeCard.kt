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
fun ChallengeCard(
    title: String, description: String, currentProgress: Int,
    targetProgress: Int, xpReward: Int, completed: Boolean = false
) {
    QuestPanel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuestIllustration(QuestArt.Scroll, Modifier.size(60.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = SqTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(description, color = SqTextSecondary, fontSize = 13.sp)
                Text(
                    if (completed) "✓ Completado" else "★ +$xpReward XP",
                    color = if (completed) SqSavings else SqXp,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
        QuestProgress(
            if (targetProgress > 0) currentProgress.toFloat() / targetProgress else 0f,
            if (completed) SqSavings else SqXp
        )
        Text("$currentProgress / $targetProgress", color = SqTextSecondary, fontSize = 12.sp)
    }
}
