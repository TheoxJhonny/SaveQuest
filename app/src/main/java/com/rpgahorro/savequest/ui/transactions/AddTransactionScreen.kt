package com.rpgahorro.savequest.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.ui.theme.SqBackground
import com.rpgahorro.savequest.ui.theme.SqExpense
import com.rpgahorro.savequest.ui.theme.SqSavings
import com.rpgahorro.savequest.ui.theme.SqTextSecondary
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment

@Composable
fun AddTransactionScreen(
    type: TransactionType,
    onBack: () -> Unit
) {

    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val isExpense = type == TransactionType.EXPENSE

    val accentColor =
        if (isExpense) SqExpense else SqSavings

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    val title =
        if (isExpense) "Registrar gasto" else "Registrar ingreso"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SqBackground)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isExpense)
                "¿En qué utilizaste tu dinero?"
            else
                "Registra el dinero que recibiste.",
            color = SqTextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() }) {
                    amount = newValue
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Monto")
            },
            prefix = {
                Text("$")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = category,
            onValueChange = {
                category = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Categoría")
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Descripción")
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                // Guardaremos realmente más adelante.
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor
            )
        ) {

            Text(
                text = if (isExpense)
                    "Registrar gasto"
                else
                    "Registrar ingreso",
                fontWeight = FontWeight.Bold
            )
        }
    }
}