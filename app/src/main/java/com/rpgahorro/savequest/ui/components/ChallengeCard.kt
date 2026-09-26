package com.rpgahorro.savequest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpgahorro.savequest.ui.theme.SqPrimary
import com.rpgahorro.savequest.ui.theme.SqSurface
import com.rpgahorro.savequest.ui.theme.SqSurfaceElevated
import com.rpgahorro.savequest.ui.theme.SqTextPrimary
import com.rpgahorro.savequest.ui.theme.SqTextSecondary

@Composable
fun ChallengeCard(
    title: String,
    description: String,
    currentProgress: Int,
    targetProgress: Int,
    xpReward: Int
) {

    val progress =
        (currentProgress.toFloat() / targetProgress.toFloat())
            .coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SqSurface)
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = title,
                color = SqTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Text(
                text = "+$xpReward XP",
                color = SqPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = description,
            color = SqTextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = SqPrimary,
            trackColor = SqSurfaceElevated
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$currentProgress / $targetProgress",
            color = SqTextSecondary,
            fontSize = 12.sp
        )
    }
}