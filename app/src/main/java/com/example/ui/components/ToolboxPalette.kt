package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KodanimaTheme
import com.example.model.ToolType
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.NeonPink
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ToolboxPalette(
    selectedTool: ToolType,
    currentColor: Long,
    brushSize: Float,
    isStylusOnlyMode: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onSelectTool: (ToolType) -> Unit,
    onChangeBrushSize: (Float) -> Unit,
    onOpenColorPicker: () -> Unit,
    onToggleStylusOnly: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetZoom: () -> Unit,
    theme: KodanimaTheme = KodanimaTheme.ROXO_BLACK,
    modifier: Modifier = Modifier
) {
    var showBrushSlider by remember { mutableStateOf(false) }

    val panelColor = Color(theme.panel)
    val accentColor = Color(theme.accent)
    val textColor = Color(theme.text)
    val borderColor = accentColor.copy(alpha = 0.35f)

    val tools = listOf(
        Pair(ToolType.PEN, Icons.Default.Edit),
        Pair(ToolType.PENCIL, Icons.Default.Timeline),
        Pair(ToolType.BRUSH, Icons.Default.Brush),
        Pair(ToolType.ERASER, Icons.Default.Clear),
        Pair(ToolType.BUCKET, Icons.Default.FormatPaint),
        Pair(ToolType.MAGIC_WAND, Icons.Default.AutoFixHigh),
        Pair(ToolType.LASSO, Icons.Default.Gesture),
        Pair(ToolType.LINE, Icons.Default.HorizontalRule),
        Pair(ToolType.RECTANGLE, Icons.Default.CropSquare),
        Pair(ToolType.CIRCLE, Icons.Default.Circle),
        Pair(ToolType.EYEDROPPER, Icons.Default.Colorize)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        // Floating Brush Size Slider Popup
        AnimatedVisibility(visible = showBrushSlider) {
            Surface(
                color = panelColor,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .width(180.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tamanho: ${brushSize.toInt()}px",
                        color = textColor,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Live circle preview
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(brushSize.coerceIn(2f, 36f).dp)
                                .clip(CircleShape)
                                .background(Color(currentColor))
                        )
                    }
                    Slider(
                        value = brushSize,
                        onValueChange = onChangeBrushSize,
                        valueRange = 1f..60f,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = accentColor.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        // Main Tool Dock
        Surface(
            color = panelColor.copy(alpha = 0.95f),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            shadowElevation = 10.dp,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Color Picker Button with current color preview
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(2.dp, accentColor, CircleShape)
                        .background(Color(currentColor))
                        .clickable { onOpenColorPicker() }
                        .testTag("tool_color_picker_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                }

                // Brush Size toggle button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showBrushSlider) accentColor.copy(alpha = 0.25f) else Color(theme.bg).copy(alpha = 0.6f))
                        .clickable { showBrushSlider = !showBrushSlider }
                        .testTag("tool_brush_size_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size((brushSize / 4f).coerceIn(4f, 16f).dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = "${brushSize.toInt()}px",
                            fontSize = 8.sp,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(1.dp)
                        .background(borderColor)
                )

                // Tool Buttons
                tools.forEach { (tool, icon) ->
                    val isSelected = tool == selectedTool
                    val btnBg by animateColorAsState(
                        targetValue = if (isSelected) accentColor else Color.Transparent,
                        label = "toolBg"
                    )
                    val iconTint = if (isSelected) Color(theme.bg) else textColor

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(btnBg)
                            .clickable { onSelectTool(tool) }
                            .testTag("tool_${tool.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tool.labelPt,
                            tint = iconTint,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(1.dp)
                        .background(borderColor)
                )

                // Undo
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("tool_undo")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Desfazer",
                        tint = if (canUndo) textColor else textColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Redo
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("tool_redo")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Refazer",
                        tint = if (canRedo) textColor else textColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Stylus / Palm Rejection Toggle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isStylusOnlyMode) OrangeAccent.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { onToggleStylusOnly() }
                        .testTag("tool_stylus_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Gesture,
                        contentDescription = if (isStylusOnlyMode) "Modo Caneta (Rejeição de Palma Ativa)" else "Modo Toque & Caneta",
                        tint = if (isStylusOnlyMode) OrangeAccent else textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Reset Zoom
                IconButton(
                    onClick = onResetZoom,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("tool_reset_zoom")
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Redefinir Zoom",
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
