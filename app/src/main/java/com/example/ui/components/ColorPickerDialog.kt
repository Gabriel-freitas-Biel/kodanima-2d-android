package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BorderSubtle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Krita-style Circular Color Wheel with Windows Kodanima complementary color harmonies.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Long,
    recentColors: List<Long>,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val argb = initialColor.toInt()
    val r = ((argb ushr 16) and 0xFF) / 255f
    val g = ((argb ushr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f

    val initialHsv = FloatArray(3)
    android.graphics.Color.RGBToHSV((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), initialHsv)

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    fun currentSelectedColor(): Long {
        val rgbInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        return (0xFF000000L or (rgbInt.toLong() and 0xFFFFFFL))
    }

    fun makeHarmonicColor(hOffset: Float): Long {
        val harmonicHue = (hue + hOffset + 360f) % 360f
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(harmonicHue, saturation, value))
        return (0xFF000000L or (rgb.toLong() and 0xFFFFFFL))
    }

    // Color Harmonies from Windows desktop Kodanima / Krita
    val directComplement = remember(hue, saturation, value) { makeHarmonicColor(180f) }
    val analogLeft = remember(hue, saturation, value) { makeHarmonicColor(-30f) }
    val analogRight = remember(hue, saturation, value) { makeHarmonicColor(30f) }
    val triad1 = remember(hue, saturation, value) { makeHarmonicColor(120f) }
    val triad2 = remember(hue, saturation, value) { makeHarmonicColor(240f) }
    val splitComp1 = remember(hue, saturation, value) { makeHarmonicColor(150f) }
    val splitComp2 = remember(hue, saturation, value) { makeHarmonicColor(210f) }

    val currentColorLong = currentSelectedColor()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("color_picker_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Roda de Cores (Estilo Krita)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Krita Circular Color Wheel with inner SV Box
                val wheelSizeDp = 220.dp
                Box(
                    modifier = Modifier
                        .size(wheelSizeDp)
                        .testTag("krita_color_wheel"),
                    contentAlignment = Alignment.Center
                ) {
                    // 1. Circular Hue Ring
                    Canvas(
                        modifier = Modifier
                            .size(wheelSizeDp)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = offset.x - center.x
                                    val dy = offset.y - center.y
                                    val dist = hypot(dx, dy)
                                    val radius = size.width / 2f
                                    val innerRadius = radius * 0.72f

                                    if (dist in (innerRadius - 10f)..(radius + 10f)) {
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < 0) angle += 360f
                                        hue = angle
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = change.position.x - center.x
                                    val dy = change.position.y - center.y
                                    val dist = hypot(dx, dy)
                                    val radius = size.width / 2f
                                    val innerRadius = radius * 0.70f

                                    if (dist in (innerRadius - 15f)..(radius + 20f)) {
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < 0) angle += 360f
                                        hue = angle
                                    }
                                }
                            }
                    ) {
                        val radius = size.width / 2f
                        val center = Offset(radius, radius)
                        val strokeWidthPx = radius * 0.28f

                        // Draw rainbow sweep ring
                        val sweepColors = listOf(
                            Color.Red, Color.Yellow, Color.Green,
                            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                        drawCircle(
                            brush = Brush.sweepGradient(sweepColors, center),
                            radius = radius - strokeWidthPx / 2f,
                            style = Stroke(width = strokeWidthPx)
                        )

                        // Draw thumb indicator for active Hue
                        val rad = Math.toRadians(hue.toDouble())
                        val thumbDist = radius - strokeWidthPx / 2f
                        val thumbX = center.x + (thumbDist * cos(rad)).toFloat()
                        val thumbY = center.y + (thumbDist * sin(rad)).toFloat()

                        drawCircle(
                            color = Color.White,
                            radius = 9.dp.toPx(),
                            center = Offset(thumbX, thumbY)
                        )
                        drawCircle(
                            color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))),
                            radius = 7.dp.toPx(),
                            center = Offset(thumbX, thumbY)
                        )
                    }

                    // 2. Central Saturation & Value Box
                    val innerBoxSize = 100.dp
                    Box(
                        modifier = Modifier
                            .size(innerBoxSize)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(innerBoxSize)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        saturation = (offset.x / size.width).coerceIn(0f, 1f)
                                        value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                        value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                    }
                                }
                        ) {
                            val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))

                            // Horizontal gradient: White -> Pure Hue
                            drawRect(
                                brush = Brush.horizontalGradient(listOf(Color.White, pureHueColor))
                            )
                            // Vertical gradient: Transparent -> Black
                            drawRect(
                                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                            )

                            // Cursor indicator
                            val cursorX = saturation * size.width
                            val cursorY = (1f - value) * size.height

                            drawCircle(
                                color = Color.White,
                                radius = 7.dp.toPx(),
                                center = Offset(cursorX, cursorY),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = Color(currentColorLong),
                                radius = 5.dp.toPx(),
                                center = Offset(cursorX, cursorY)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Color Preview & Hex Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(currentColorLong))
                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Cor Atual",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "#${java.lang.Long.toHexString(currentColorLong).uppercase().takeLast(6)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onDismiss) { Text("Cancelar") }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onColorSelected(currentColorLong)
                                }
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Selecionar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Complementary & Harmonic Colors (Desktop Windows / Krita Harmonies)
                Text(
                    text = "Cores Complementares & Harmonias (Windows Kodanima)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HarmonicColorChip("Complementar", directComplement) {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(it.toInt(), hsv)
                        hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                    }
                    HarmonicColorChip("Análoga 1", analogLeft) {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(it.toInt(), hsv)
                        hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                    }
                    HarmonicColorChip("Análoga 2", analogRight) {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(it.toInt(), hsv)
                        hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                    }
                    HarmonicColorChip("Tríade 1", triad1) {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(it.toInt(), hsv)
                        hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                    }
                    HarmonicColorChip("Tríade 2", triad2) {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(it.toInt(), hsv)
                        hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                    }
                    HarmonicColorChip("Dividida 1", splitComp1) {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(it.toInt(), hsv)
                        hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                    }
                    HarmonicColorChip("Dividida 2", splitComp2) {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(it.toInt(), hsv)
                        hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Recent colors
                Text(
                    text = "Cores Recentes",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                .clickable {
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(c.toInt(), hsv)
                                    hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HarmonicColorChip(
    label: String,
    colorLong: Long,
    onClick: (Long) -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick(colorLong) }
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(colorLong))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
