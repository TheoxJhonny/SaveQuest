package com.rpgahorro.savequest.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.rpgahorro.savequest.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun XpProgressCard(level: Int, currentXp: Int, requiredXp: Int) {
    var previousLevel by remember { mutableIntStateOf(level) }
    var celebrate by remember { mutableStateOf(false) }
    LaunchedEffect(level) {
        if (level > previousLevel) {
            celebrate = true
            delay(1600)
            celebrate = false
        }
        previousLevel = level
    }
    val tint by animateColorAsState(
        if (celebrate) SqTextPrimary else SqXp,
        tween(500),
        label = "Subida de nivel"
    )
    QuestPanel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
                QuestIllustration(QuestArt.Shield, Modifier.fillMaxSize())
                Text(
                    "$level",
                    modifier = Modifier.padding(top = 10.dp),
                    color = tint,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (celebrate) "¡NIVEL $level!" else "NIVEL $level",
                        color = tint,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text("$currentXp / $requiredXp XP", color = SqTextSecondary, fontSize = 11.sp)
                }
                QuestProgress(if (requiredXp > 0) currentXp.toFloat() / requiredXp else 0f, SqXp)
            }
        }
    }
}
