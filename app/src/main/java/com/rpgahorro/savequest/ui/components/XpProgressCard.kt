package com.rpgahorro.savequest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpgahorro.savequest.ui.theme.SqSurface
import com.rpgahorro.savequest.ui.theme.SqTextSecondary
import com.rpgahorro.savequest.ui.theme.SqXp

@Composable
fun XpProgressCard(
    level: Int,
    currentXp: Int,
    requiredXp: Int
) {

    val progress =
        (currentXp.toFloat() / requiredXp.toFloat())
            .coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SqSurface)
            .padding(20.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "NIVEL $level",
                color = SqXp,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                text = "$currentXp / $requiredXp XP",
                color = SqTextSecondary,
                fontSize = 13.sp
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = SqXp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}