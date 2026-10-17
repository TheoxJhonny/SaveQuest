package com.rpgahorro.savequest.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rpgahorro.savequest.ui.components.*
import com.rpgahorro.savequest.ui.theme.*

@Composable
fun MapScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        QuestLabel("EXPLORA TU CAMINO")
        Text("Mapa", style = MaterialTheme.typography.headlineLarge, color = SqTextPrimary)
        QuestPanel {
            Icon(Icons.Outlined.Map, null, Modifier.size(48.dp), tint = SqPrimarySoft)
            Text("Un nuevo territorio por descubrir", style = MaterialTheme.typography.titleLarge)
            Text("El mapa estará disponible en una próxima etapa.", color = SqTextSecondary)
        }
    }
}
