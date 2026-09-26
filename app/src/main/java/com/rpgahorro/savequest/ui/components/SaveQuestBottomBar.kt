package com.rpgahorro.savequest.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.rpgahorro.savequest.navigation.SaveQuestDestination
import com.rpgahorro.savequest.navigation.bottomDestinations
import com.rpgahorro.savequest.ui.theme.SqPrimary
import com.rpgahorro.savequest.ui.theme.SqSurface
import com.rpgahorro.savequest.ui.theme.SqTextSecondary

@Composable
fun SaveQuestBottomBar(
    currentRoute: String?,
    onNavigate: (SaveQuestDestination) -> Unit
) {

    NavigationBar(
        containerColor = SqSurface
    ) {

        bottomDestinations.forEach { destination ->

            val selected = currentRoute == destination.route

            NavigationBarItem(
                selected = selected,

                onClick = {
                    onNavigate(destination)
                },

                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },

                label = {
                    Text(
                        text = destination.label,
                        fontSize = 10.sp
                    )
                },

                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SqPrimary,
                    selectedTextColor = SqPrimary,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = SqTextSecondary,
                    unselectedTextColor = SqTextSecondary
                )
            )
        }
    }
}