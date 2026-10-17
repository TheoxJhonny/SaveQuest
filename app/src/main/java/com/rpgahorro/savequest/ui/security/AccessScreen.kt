package com.rpgahorro.savequest.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.theme.*

@Composable
fun AccessScreen(message: String, busy: Boolean, canAuthenticate: Boolean, canEnroll: Boolean,
                 onUnlock: () -> Unit, onConfigure: () -> Unit, onRetry: () -> Unit) {
    Surface(color = SqBackground) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            QuestIllustration(QuestArt.Crystal, Modifier.size(80.dp))
            QuestLabel("SAVEQUEST")
            Spacer(Modifier.height(24.dp))
            QuestPanel {
                Icon(Icons.Outlined.Fingerprint, null, Modifier.size(42.dp), tint = SqPrimarySoft)
                Text("Tu aventura, protegida", style = MaterialTheme.typography.headlineMedium)
                Text(message, color = SqTextSecondary)
                QuestButton(if (busy) "Esperando autenticación…" else "Desbloquear", onUnlock, canAuthenticate && !busy)
                if (canEnroll) TextButton(onConfigure, enabled = !busy) { Text("Configurar biometría") }
                if (!canAuthenticate) TextButton(onRetry) { Text("Comprobar nuevamente") }
            }
            Spacer(Modifier.height(16.dp))
            Text("Tus registros permanecen en este dispositivo.", color = SqTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
