package com.rpgahorro.savequest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpgahorro.savequest.ui.theme.SqExpense
import com.rpgahorro.savequest.ui.theme.SqPrimary
import com.rpgahorro.savequest.ui.theme.SqSavings
import com.rpgahorro.savequest.ui.theme.SqSurface
import com.rpgahorro.savequest.ui.theme.SqTextPrimary
import com.rpgahorro.savequest.ui.theme.SqTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovementSheet(
    onDismiss: () -> Unit,
    onExpenseClick: () -> Unit,
    onIncomeClick: () -> Unit,
    onGoalContributionClick: () -> Unit,
    onSharedExpenseClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SqSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 32.dp
                )
        ) {
            Text(
                text = "Registrar movimiento",
                color = SqTextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "¿Qué quieres registrar?",
                color = SqTextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            MovementOption(
                title = "Gasto",
                description = "Registra una compra o pago.",
                icon = Icons.Outlined.RemoveCircle,
                iconColor = SqExpense,
                onClick = onExpenseClick
            )

            HorizontalDivider()
            MovementOption("Gasto compartido", "Productos, participantes y deudas.", Icons.Outlined.RemoveCircle, SqPrimary, onSharedExpenseClick)
            HorizontalDivider()

            MovementOption(
                title = "Ingreso",
                description = "Registra dinero que recibiste.",
                icon = Icons.Outlined.AddCircle,
                iconColor = SqSavings,
                onClick = onIncomeClick
            )

            HorizontalDivider()

            MovementOption(
                title = "Aporte a meta",
                description = "Acércate a uno de tus objetivos.",
                icon = Icons.Outlined.Savings,
                iconColor = SqPrimary,
                onClick = onGoalContributionClick
            )
        }
    }
}

@Composable
private fun MovementOption(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor
        )

        Column(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Text(
                text = title,
                color = SqTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                color = SqTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}