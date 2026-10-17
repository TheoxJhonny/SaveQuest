package com.rpgahorro.savequest.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import com.rpgahorro.savequest.navigation.*
import com.rpgahorro.savequest.ui.theme.*

@Composable
fun SaveQuestBottomBar(currentRoute: String?, onNavigate: (SaveQuestDestination) -> Unit) {
    val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    NavigationBar(modifier = Modifier.clip(shape).border(1.dp, Color(0xFF44366B), shape), containerColor = SqSurface) {
        bottomDestinations.forEach { destination ->
            NavigationBarItem(selected = currentRoute == destination.route, onClick = { onNavigate(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label, fontSize = 10.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = SqPrimarySoft,
                    selectedTextColor = SqPrimarySoft, indicatorColor = SqPrimary.copy(alpha = .15f),
                    unselectedIconColor = SqTextSecondary, unselectedTextColor = SqTextSecondary))
        }
    }
}
