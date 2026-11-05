package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.KodanimaTheme

@Composable
fun MyApplicationTheme(
    kodanimaTheme: KodanimaTheme = KodanimaTheme.ROXO_BLACK,
    content: @Composable () -> Unit
) {
    val isLight = kodanimaTheme == KodanimaTheme.BRANCO_CINZA_AZULADO
    val colorScheme = if (isLight) {
        lightColorScheme(
            primary = Color(kodanimaTheme.accent),
            onPrimary = Color.White,
            primaryContainer = Color(kodanimaTheme.accent).copy(alpha = 0.2f),
            onPrimaryContainer = Color(kodanimaTheme.text),
            secondary = OrangeAccent,
            background = Color(kodanimaTheme.bg),
            onBackground = Color(kodanimaTheme.text),
            surface = Color(kodanimaTheme.panel),
            onSurface = Color(kodanimaTheme.text),
            surfaceVariant = Color(0xFFE2E4EC),
            onSurfaceVariant = Color(0xFF555966),
            surfaceContainerHighest = Color(0xFFD6DAE6),
            outline = Color(0xFFC4C8D4),
            outlineVariant = Color(0xFFB5BAC8)
        )
    } else {
        darkColorScheme(
            primary = Color(kodanimaTheme.accent),
            onPrimary = Color(kodanimaTheme.bg),
            primaryContainer = Color(kodanimaTheme.accent).copy(alpha = 0.25f),
            onPrimaryContainer = Color(kodanimaTheme.text),
            secondary = OrangeAccent,
            background = Color(kodanimaTheme.bg),
            onBackground = Color(kodanimaTheme.text),
            surface = Color(kodanimaTheme.panel),
            onSurface = Color(kodanimaTheme.text),
            surfaceVariant = Color(kodanimaTheme.panel).copy(alpha = 0.85f),
            onSurfaceVariant = Color(kodanimaTheme.text).copy(alpha = 0.75f),
            surfaceContainerHighest = Color(kodanimaTheme.panel).copy(alpha = 0.95f),
            outline = Color(kodanimaTheme.accent).copy(alpha = 0.35f),
            outlineVariant = Color(0xFF383E52)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
