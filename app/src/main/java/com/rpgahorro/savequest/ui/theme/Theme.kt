package com.rpgahorro.savequest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SaveQuestColorScheme = darkColorScheme(
    primary = SqPrimary,
    secondary = SqSavings,
    tertiary = SqXp,

    background = SqBackground,
    surface = SqSurface,

    onPrimary = SqTextPrimary,
    onSecondary = SqBackground,
    onBackground = SqTextPrimary,
    onSurface = SqTextPrimary
)

@Composable
fun SaveQuestTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SaveQuestColorScheme,
        typography = Typography,
        content = content
    )
}