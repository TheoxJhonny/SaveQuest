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
import com.rpgahorro.savequest.ui.theme.SqSavings
import com.rpgahorro.savequest.ui.theme.SqSurface
import com.rpgahorro.savequest.ui.theme.SqSurfaceElevated
import com.rpgahorro.savequest.ui.theme.SqTextPrimary
import com.rpgahorro.savequest.ui.theme.SqTextSecondary

@Composable
fun SavingGoalCard(
    name: String,
    currentAmount: Int,
    targetAmount: Int
) {

    val progress =
        (currentAmount.toFloat() / targetAmount.toFloat())
            .coerceIn(0f, 1f)

    val percentage = (progress * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SqSurface)
            .padding(20.dp)
    ) {

        Text(
            text = "META PRINCIPAL",
            color = SqPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = name,
                color = SqTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$percentage%",
                color = SqSavings,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = SqSavings,
            trackColor = SqSurfaceElevated
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "$${formatMoney(currentAmount)} ahorrados",
                color = SqTextSecondary,
                fontSize = 13.sp
            )

            Text(
                text = "Meta: $${formatMoney(targetAmount)}",
                color = SqTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

private fun formatMoney(amount: Int): String {
    return "%,d".format(amount).replace(",", ".")
}