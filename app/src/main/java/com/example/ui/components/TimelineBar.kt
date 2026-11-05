package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Frame
import com.example.model.KodanimaTheme
import com.example.ui.theme.NeonPink
import com.example.ui.theme.OrangeAccent

@Composable
fun TimelineBar(
    frames: List<Frame>,
    currentFrameIndex: Int,
    isPlaying: Boolean,
    onionSkinEnabled: Boolean,
    layerCount: Int,
    fps: Int,
    onSelectFrame: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    onNextFrame: () -> Unit,
    onPrevFrame: () -> Unit,
    onAddFrame: () -> Unit,
    onDuplicateFrame: () -> Unit,
    onDeleteFrame: () -> Unit,
    onToggleOnionSkin: () -> Unit,
    onOpenLayers: () -> Unit,
    onOpenSettings: () -> Unit,
    theme: KodanimaTheme = KodanimaTheme.ROXO_BLACK,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val panelColor = Color(theme.panel)
    val bgColor = Color(theme.bg)
    val accentColor = Color(theme.accent)
    val textColor = Color(theme.text)
    val subtleBorder = accentColor.copy(alpha = 0.3f)

    // Keep active frame in view
    LaunchedEffect(currentFrameIndex) {
        if (currentFrameIndex in frames.indices) {
            listState.animateScrollToItem(currentFrameIndex)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = panelColor,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // Upper row: Playback controls, frame counter, onion skin & layers toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Playback button group
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    IconButton(
                        onClick = onPrevFrame,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("timeline_prev_button")
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Quadro anterior",
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) OrangeAccent else accentColor)
                            .clickable { onTogglePlay() }
                            .testTag("timeline_play_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            tint = bgColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextFrame,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("timeline_next_button")
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Próximo quadro",
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Frame Indicator Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor.copy(alpha = 0.7f))
                            .border(0.5.dp, subtleBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${currentFrameIndex + 1} / ${frames.size}",
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // FPS indicator (clickable to open settings)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .clickable { onOpenSettings() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$fps FPS",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Action buttons: Onion Skin, Layers, Add, Duplicate, Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Onion Skin (Camada Cebola) toggle
                    val onionBg by animateColorAsState(
                        targetValue = if (onionSkinEnabled) NeonPink.copy(alpha = 0.25f) else bgColor.copy(alpha = 0.5f),
                        label = "onionSkinBg"
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(onionBg)
                            .border(
                                1.dp,
                                if (onionSkinEnabled) NeonPink else subtleBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onToggleOnionSkin() }
                            .testTag("timeline_onion_skin_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "Camada Cebola (Onion Skin)",
                                tint = if (onionSkinEnabled) NeonPink else textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Cebola",
                                fontSize = 8.sp,
                                color = if (onionSkinEnabled) NeonPink else textColor.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Layers sheet toggle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor.copy(alpha = 0.6f))
                            .border(1.dp, subtleBorder, RoundedCornerShape(8.dp))
                            .clickable { onOpenLayers() }
                            .testTag("timeline_layers_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = accentColor, contentColor = bgColor) {
                                    Text("$layerCount", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = "Gerenciar Camadas",
                                tint = textColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Duplicate frame button
                    IconButton(
                        onClick = onDuplicateFrame,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("timeline_duplicate_frame")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Duplicar quadro",
                            tint = textColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Delete frame button
                    IconButton(
                        onClick = onDeleteFrame,
                        enabled = frames.size > 1,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("timeline_delete_frame")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Excluir quadro",
                            tint = if (frames.size > 1) OrangeAccent else textColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Add new frame (+) button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor)
                            .clickable { onAddFrame() }
                            .testTag("timeline_add_frame"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Adicionar quadro",
                            tint = bgColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Lower row: Frame Strip
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                itemsIndexed(frames) { index, frame ->
                    val isSelected = index == currentFrameIndex
                    val strokeCount = frame.layers.sumOf { it.strokes.size }

                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) accentColor else subtleBorder,
                        label = "frameBorderColor"
                    )
                    val cellBg = if (isSelected) accentColor.copy(alpha = 0.28f) else bgColor.copy(alpha = 0.6f)

                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cellBg)
                            .border(if (isSelected) 1.5.dp else 0.5.dp, borderColor, RoundedCornerShape(6.dp))
                            .clickable { onSelectFrame(index) }
                            .testTag("timeline_frame_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) accentColor else textColor
                            )
                            if (strokeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) accentColor else textColor.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
