package com.rpgahorro.savequest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpgahorro.savequest.ui.theme.SqExpense
import com.rpgahorro.savequest.ui.theme.SqSavings
import com.rpgahorro.savequest.ui.theme.SqTextPrimary
import com.rpgahorro.savequest.ui.theme.SqTextSecondary

@Composable
fun TransactionItem(
    title: String,
    category: String,
    amount: Long,
    isIncome: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column {

            Text(
                text = title,
                color = SqTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = category,
                color = SqTextSecondary,
                fontSize = 12.sp
            )
        }

        Text(
            text = if (isIncome) {
                "+$${formatTransactionMoney(amount)}"
            } else {
                "-$${formatTransactionMoney(amount)}"
            },
            color = if (isIncome) SqSavings else SqExpense,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

private fun formatTransactionMoney(amount: Long): String {
    return "%,d".format(amount).replace(",", ".")
}