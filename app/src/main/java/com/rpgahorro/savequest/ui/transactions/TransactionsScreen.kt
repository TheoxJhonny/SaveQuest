package com.rpgahorro.savequest.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.ui.theme.SqBackground
import com.rpgahorro.savequest.ui.theme.SqExpense
import com.rpgahorro.savequest.ui.theme.SqSavings
import com.rpgahorro.savequest.ui.theme.SqTextPrimary
import com.rpgahorro.savequest.ui.theme.SqTextSecondary

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SqBackground)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        item {

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Movimientos",
                color = SqTextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tu actividad financiera",
                color = SqTextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        if (transactions.isEmpty()) {

            item {

                Text(
                    text = "Todavía no tienes movimientos.",
                    color = SqTextSecondary
                )
            }

        } else {

            items(
                items = transactions,
                key = { transaction -> transaction.id }
            ) { transaction ->

                TransactionRow(transaction)

                HorizontalDivider()
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity
) {

    val isExpense =
        transaction.type == TransactionType.EXPENSE.name

    val amountColor =
        if (isExpense) SqExpense else SqSavings

    val prefix =
        if (isExpense) "-" else "+"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = transaction.category,
                color = SqTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            if (transaction.description.isNotBlank()) {

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = transaction.description,
                    color = SqTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Text(
            text = "$prefix$${formatAmount(transaction.amount)}",
            color = amountColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

private fun formatAmount(
    amount: Long
): String {

    return "%,d"
        .format(amount)
        .replace(',', '.')
}