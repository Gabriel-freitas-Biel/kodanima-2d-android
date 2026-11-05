package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.AppUiMode
import com.example.model.DrawingStroke
import com.example.model.KodanimaTheme
import com.example.model.Layer
import com.example.model.TimelineLayoutMode
import com.example.model.ToolType
import com.example.ui.components.CanvasView
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.ProjectSettingsDialog
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorDialog
import com.example.viewmodel.KodanimaViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Authentic Desktop PC Studio Interface for Kodanima 2D.
 *
 * Faithfully reproduces the C++ Kodanima 2D layout as seen in the user's reference screenshots:
 * - Floating left vertical toolbar with the 7 core tools (Pencil, Eraser, Bucket, Lasso, Color Wheel, Eyedropper, Clapperboard/Render).
 * - Canvas in viewport with 1-finger draw, 2-finger zoom/pan/rotate.
 * - Top header with canvas rotation reset chip, layout switcher, and undo/redo.
 * - Two Timeline Modes:
 *     1) Heritage Junior: Compact floating timeline window centered at the bottom.
 *     2) Heritage Profissional: Full-width edge-to-edge timeline spanning the entire screen width.
 * - Comprehensive timeline controls matching the desktop screenshot:
 *     Layer (+, -, up, down, eye), Themes, Color dot, Onion skin (%, opacity), Play/Loop, Frame (+, Insert, Delete),
 *     Media import (Audio/Video), Audio scrub, Duration, FPS, and total time calculation.
 */
@Composable
fun PcEditorScreen(
    viewModel: KodanimaViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentTheme = uiState.preferences.theme

    BackHandler {
        viewModel.saveCurrentProject(context)
        viewModel.navigateTo(AppScreen.HOME)
    }

    var showOnionOpacityDialog by remember { mutableStateOf(false) }
    var showFpsDialog by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var showMediaImportChoice by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    // Media Pickers
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importAudioUri(uri.toString())
            Toast.makeText(context, "Áudio importado para a timeline!", Toast.LENGTH_SHORT).show()
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setReferenceImageUri(uri.toString())
            Toast.makeText(context, "Mídia de referência importada!", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(currentTheme.bg))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("pc_editor_screen")
            .focusable()
            .onKeyEvent { keyEvent ->
                viewModel.handlePhysicalKeyEvent(keyEvent.nativeKeyEvent, context)
            }
    ) {
        // 1. Viewport Canvas (Draw & Multi-touch)
        CanvasView(
            project = uiState.project,
            currentFrameIndex = uiState.currentFrameIndex,
            selectedTool = uiState.selectedTool,
            currentColor = uiState.currentColor,
            brushSize = uiState.brushSize,
            brushOpacity = uiState.brushOpacity,
            onionSkinEnabled = uiState.onionSkinEnabled,
            isStylusOnlyMode = uiState.isStylusOnlyMode,
            zoom = uiState.zoom,
            panX = uiState.panX,
            panY = uiState.panY,
            rotation = uiState.rotation,
            activeSelection = uiState.activeSelection,
            onTransformChange = { zoomChange, pX, pY, rotChange ->
                viewModel.updateTransform(zoomChange, pX, pY, rotChange)
            },
            onStrokeCompleted = { stroke ->
                viewModel.addStrokeToCurrentLayer(stroke)
            },
            onEyedropperColorPicked = { pickedColor ->
                viewModel.setColor(pickedColor)
                viewModel.selectTool(ToolType.PEN)
                Toast.makeText(context, "Cor capturada!", Toast.LENGTH_SHORT).show()
            },
            onBucketFill = { x, y ->
                viewModel.performBucketFill(x, y)
            },
            onMagicWand = { x, y ->
                viewModel.performMagicWand(x, y)
            },
            onLassoCompleted = { lassoPoints ->
                viewModel.setLassoSelection(lassoPoints)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Header Bar (Floating unobtrusive desktop bar)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = Color(currentTheme.panel).copy(alpha = 0.92f),
            tonalElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(currentTheme.accent).copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back to Home, Project name, and Canvas Rotation Reset Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            viewModel.saveCurrentProject(context)
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("pc_back_home_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar aos Projetos",
                            tint = Color(currentTheme.text),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Project Title
                    Text(
                        text = uiState.project.name,
                        color = Color(currentTheme.text),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            viewModel.openDialog(EditorDialog.PROJECT_SETTINGS)
                        }
                    )

                    // Rotation Indicator & Reset Button (When canvas is rotated, clicking it resets rotation to 0°)
                    val rotDeg = uiState.rotation.roundToInt()
                    if (abs(uiState.rotation) > 0.5f) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    viewModel.resetRotation()
                                    Toast.makeText(context, "Giro zerado (0°)", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("canvas_rotation_reset_btn"),
                            color = OrangeAccent.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeAccent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.RestartAlt,
                                    contentDescription = "Zerar giro",
                                    tint = OrangeAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Giro: ${rotDeg}° (Zerar)",
                                    color = OrangeAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Zoom indicator (click to reset zoom/pan)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.resetCanvasTransform() },
                        color = Color(currentTheme.panel),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(currentTheme.accent).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${(uiState.zoom * 100).toInt()}%",
                            color = Color(currentTheme.text).copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Center-Right: Undo/Redo & Layout Mode Switcher (Junior vs Profissional)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Undo
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Desfazer (Ctrl+Z)",
                            tint = if (uiState.canUndo) Color(currentTheme.text) else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Refazer (Ctrl+Y)",
                            tint = if (uiState.canRedo) Color(currentTheme.text) else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Timeline Layout Toggle: Heritage Junior <-> Heritage Profissional
                    val isProf = uiState.preferences.timelineLayoutMode == TimelineLayoutMode.HERITAGE_PROFISSIONAL
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val nextMode = if (isProf) TimelineLayoutMode.HERITAGE_JUNIOR else TimelineLayoutMode.HERITAGE_PROFISSIONAL
                                viewModel.setTimelineLayoutMode(nextMode)
                            }
                            .testTag("toggle_timeline_layout_btn"),
                        color = Color(currentTheme.accent).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(currentTheme.accent))
                    ) {
                        Text(
                            text = if (isProf) "Profissional" else "Junior",
                            color = Color(currentTheme.accent),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Switch back to Mobile Mode
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.setUiMode(AppUiMode.MOBILE) },
                        color = Color(currentTheme.panel),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = "Modo Mobile",
                            color = Color(currentTheme.text).copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Settings Gear
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = Color(currentTheme.text),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. Floating Left Vertical Toolbar (7 Classic Kodanima 2D Tools from Desktop Screenshots)
        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .shadow(12.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(currentTheme.accent).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .testTag("pc_floating_toolbar"),
            color = Color(currentTheme.panel).copy(alpha = 0.95f),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Lápis / Caneta (L)
                DesktopToolButton(
                    icon = Icons.Default.Brush,
                    tooltip = "Lápis (B)",
                    isSelected = uiState.selectedTool == ToolType.PEN,
                    accentColor = Color(currentTheme.accent),
                    onClick = { viewModel.selectTool(ToolType.PEN) }
                )

                // 2. Borracha (E)
                DesktopToolButton(
                    icon = Icons.Default.Gesture,
                    tooltip = "Borracha (E)",
                    isSelected = uiState.selectedTool == ToolType.ERASER,
                    accentColor = NeonPink,
                    onClick = { viewModel.selectTool(ToolType.ERASER) }
                )

                // 3. Balde de Tinta (G)
                DesktopToolButton(
                    icon = Icons.Default.FormatPaint,
                    tooltip = "Balde de Tinta (G)",
                    isSelected = uiState.selectedTool == ToolType.BUCKET,
                    accentColor = CyanAccent,
                    onClick = { viewModel.selectTool(ToolType.BUCKET) }
                )

                // 4. Varinha Mágica (W)
                DesktopToolButton(
                    icon = Icons.Default.AutoFixHigh,
                    tooltip = "Varinha Mágica (W)",
                    isSelected = uiState.selectedTool == ToolType.MAGIC_WAND,
                    accentColor = Color(currentTheme.accent),
                    onClick = { viewModel.selectTool(ToolType.MAGIC_WAND) }
                )

                // 5. Laço de Seleção (S)
                DesktopToolButton(
                    icon = Icons.Default.Speed,
                    tooltip = "Laço de Seleção (S)",
                    isSelected = uiState.selectedTool == ToolType.LASSO,
                    accentColor = OrangeAccent,
                    onClick = { viewModel.selectTool(ToolType.LASSO) }
                )

                // 5. Roda de Cores / Paleta
                DesktopToolButton(
                    icon = Icons.Default.ColorLens,
                    tooltip = "Roda de Cores",
                    isSelected = false,
                    accentColor = Color(uiState.currentColor),
                    onClick = { viewModel.openDialog(EditorDialog.COLOR_PICKER) }
                )

                // 6. Conta-Gotas (I)
                DesktopToolButton(
                    icon = Icons.Default.Colorize,
                    tooltip = "Conta-Gotas (I)",
                    isSelected = uiState.selectedTool == ToolType.EYEDROPPER,
                    accentColor = NeonGreen,
                    onClick = { viewModel.selectTool(ToolType.EYEDROPPER) }
                )

                // 7. Renderizar / Claquete de Vídeo (F12)
                DesktopToolButton(
                    icon = Icons.Default.Movie,
                    tooltip = "Renderizar / Exportar (F12)",
                    isSelected = false,
                    accentColor = Color(currentTheme.accent),
                    onClick = { viewModel.openDialog(EditorDialog.EXPORT) }
                )
            }
        }

        // 4. Timeline at Bottom (Heritage Junior vs Heritage Profissional)
        val isProf = uiState.preferences.timelineLayoutMode == TimelineLayoutMode.HERITAGE_PROFISSIONAL
        val totalSecs = (uiState.project.frames.size / uiState.project.fps.toFloat()).coerceAtLeast(0.041f)
        val audioAsset = uiState.project.mediaAssets.firstOrNull { it.type == "audio" }
        val videoAsset = uiState.project.mediaAssets.firstOrNull { it.type == "video" || it.type == "gif" }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(
                    if (isProf) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(0.85f)
                    }
                )
                .shadow(16.dp, if (isProf) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp))
                .clip(if (isProf) RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp) else RoundedCornerShape(10.dp))
                .border(
                    width = 1.dp,
                    color = Color(currentTheme.accent).copy(alpha = 0.45f),
                    shape = if (isProf) RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp) else RoundedCornerShape(10.dp)
                )
                .background(Color(currentTheme.panel).copy(alpha = 0.98f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Timeline Top Ribbon Toolbar (Layer buttons, Onion skin, Play/Loop, Frame ops, Media)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(currentTheme.bg).copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // [+] Add Layer
                    TimelineIconButton(icon = Icons.Default.Add, tooltip = "Nova camada") {
                        viewModel.addLayer()
                    }

                    // [🗑] Delete Layer
                    TimelineIconButton(icon = Icons.Default.Delete, tooltip = "Excluir camada") {
                        viewModel.deleteLayer(uiState.activeLayerIndex)
                    }

                    // [↑] Move Up
                    TimelineIconButton(icon = Icons.Default.KeyboardArrowUp, tooltip = "Subir camada") {
                        viewModel.moveLayerUp(uiState.activeLayerIndex)
                    }

                    // [↓] Move Down
                    TimelineIconButton(icon = Icons.Default.KeyboardArrowDown, tooltip = "Descer camada") {
                        viewModel.moveLayerDown(uiState.activeLayerIndex)
                    }

                    // [👁] Visibility Toggle
                    val currLayer = uiState.project.frames.getOrNull(uiState.currentFrameIndex)?.layers?.getOrNull(uiState.activeLayerIndex)
                    val isVis = currLayer?.isVisible ?: true
                    TimelineIconButton(
                        icon = if (isVis) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        tooltip = "Visibilidade da camada"
                    ) {
                        viewModel.toggleLayerVisibility(uiState.activeLayerIndex)
                    }

                    // [🎨] Theme quick switch
                    TimelineIconButton(icon = Icons.Default.ColorLens, tooltip = "Mudar Tema") {
                        showThemeMenu = true
                    }

                    // [●] Color dot preview
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(uiState.currentColor))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .clickable { viewModel.openDialog(EditorDialog.COLOR_PICKER) }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Onion Skin icon & % value
                    val onionActive = uiState.onionSkinEnabled && uiState.project.onionSkin.isEnabled
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (onionActive) Color(currentTheme.accent).copy(alpha = 0.25f) else Color.Transparent)
                            .border(1.dp, if (onionActive) Color(currentTheme.accent) else BorderSubtle, RoundedCornerShape(4.dp))
                            .clickable { viewModel.toggleOnionSkin() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = "Onion Skin",
                            tint = if (onionActive) Color(currentTheme.accent) else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "% ${(uiState.project.onionSkin.opacity * 100).toInt()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (onionActive) Color(currentTheme.accent) else TextMuted,
                            modifier = Modifier.clickable { showOnionOpacityDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // [▶ / ⏸] Play / Pause
                    TimelineIconButton(
                        icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        tooltip = if (uiState.isPlaying) "Pausar" else "Reproduzir",
                        tint = if (uiState.isPlaying) OrangeAccent else Color(currentTheme.text)
                    ) {
                        viewModel.togglePlayback()
                    }

                    // [🔁] Loop toggle
                    TimelineIconButton(
                        icon = Icons.Default.Repeat,
                        tooltip = "Repetição contínua (Loop)",
                        tint = if (uiState.isLooping) Color(currentTheme.accent) else TextMuted
                    ) {
                        viewModel.toggleLoop()
                    }

                    // [+ Quadro] Add Frame
                    TimelineTextButton(text = "+ Quadro", tooltip = "Adicionar quadro no final") {
                        viewModel.addNewFrame()
                    }

                    // [Inserir] Insert Frame
                    TimelineTextButton(text = "Inserir", tooltip = "Inserir quadro nesta posição") {
                        viewModel.insertFrameAtCurrentPosition()
                    }

                    // [✕] Delete Frame
                    TimelineTextButton(text = "✕", tooltip = "Excluir quadro atual", textColor = NeonPink) {
                        viewModel.deleteCurrentFrame()
                    }

                    // [📥] Import media
                    TimelineIconButton(icon = Icons.Default.Download, tooltip = "Importar Áudio / Vídeo") {
                        showMediaImportChoice = true
                    }

                    // [🔊] Audio Scrub toggle
                    val hasAudio = uiState.project.mediaAssets.any { it.type == "audio" }
                    TimelineIconButton(
                        icon = if (hasAudio) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        tooltip = "Áudio da Timeline",
                        tint = if (hasAudio) NeonGreen else TextMuted
                    ) {
                        if (!hasAudio) {
                            audioPicker.launch("audio/*")
                        } else {
                            Toast.makeText(context, "Áudio já carregado na timeline", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Dur: 1.000
                    val currentFrame = uiState.project.frames.getOrNull(uiState.currentFrameIndex)
                    val frameDurSec = (currentFrame?.holdFrames?.toDouble() ?: 1.0) / uiState.project.fps
                    Text(
                        text = "Dur: ${String.format("%.3f", frameDurSec)}",
                        fontSize = 10.sp,
                        color = Color(currentTheme.text).copy(alpha = 0.85f),
                        modifier = Modifier
                            .clickable { showDurationDialog = true }
                            .padding(horizontal = 4.dp)
                    )

                    // FPS: 24
                    Text(
                        text = "FPS: ${uiState.project.fps}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(currentTheme.accent),
                        modifier = Modifier
                            .clickable { showFpsDialog = true }
                            .padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Total count text: "Total: 1 quadro | 1.000 s"
                    Text(
                        text = "Total: ${uiState.project.frames.size} quadros | ${String.format("%.3f", totalSecs)} s",
                        fontSize = 10.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                // Timeline Rows (Left Column + Right Frame Track)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    // Left Column (Camadas, Música, Vídeo / GIF)
                    Column(
                        modifier = Modifier
                            .width(130.dp)
                            .fillMaxHeight()
                            .background(Color(currentTheme.panel))
                            .border(androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle))
                    ) {
                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(Color(currentTheme.bg))
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "CAMADAS / ORDEM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = TextMuted
                            )
                        }

                        // Layers list
                        val currFrameObj = uiState.project.frames.getOrNull(uiState.currentFrameIndex)
                        val layers = currFrameObj?.layers ?: emptyList()

                        Box(modifier = Modifier.weight(1f)) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(layers) { index, layer ->
                                    val isSelected = index == uiState.activeLayerIndex
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(26.dp)
                                            .background(
                                                if (isSelected) Color(currentTheme.accent).copy(alpha = 0.25f)
                                                else Color.Transparent
                                            )
                                            .clickable { viewModel.selectLayer(index) }
                                            .padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(currentTheme.accent) else TextMuted,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { viewModel.toggleLayerVisibility(index) }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = layer.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(currentTheme.accent) else Color(currentTheme.text),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Música (Audio Layer)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .background(Color(currentTheme.bg).copy(alpha = 0.5f))
                                .clickable {
                                    if (audioAsset == null) audioPicker.launch("audio/*")
                                    else Toast.makeText(context, "Faixa de áudio ativa: ${audioAsset.name}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = if (audioAsset != null) NeonGreen else TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (audioAsset != null) audioAsset.name else "Música",
                                fontSize = 10.sp,
                                color = if (audioAsset != null) NeonGreen else TextMuted,
                                maxLines = 1
                            )
                        }

                        // Vídeo / GIF Layer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .background(Color(currentTheme.bg).copy(alpha = 0.5f))
                            .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    if (videoAsset == null) videoPicker.launch("video/*")
                                    else Toast.makeText(context, "Mídia de fundo ativa: ${videoAsset.name}", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    Icons.Default.VideoFile,
                                    contentDescription = null,
                                    tint = if (videoAsset != null) OrangeAccent else TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Vídeo / ...",
                                    fontSize = 10.sp,
                                    color = if (videoAsset != null) OrangeAccent else TextMuted
                                )
                            }

                            // Desktop sub-buttons: tr (transform), a% (opacity), reset
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "tr",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(currentTheme.text),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(BorderSubtle)
                                        .padding(horizontal = 2.dp)
                                        .clickable {
                                            if (videoAsset != null) viewModel.updateSpecialAssetScale(videoAsset.id, 120.0)
                                            else videoPicker.launch("video/*")
                                        }
                                )
                                Text(
                                    text = "a%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(currentTheme.text),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(BorderSubtle)
                                        .padding(horizontal = 2.dp)
                                        .clickable {
                                            if (videoAsset != null) viewModel.updateSpecialAssetOpacity(videoAsset.id, 0.5)
                                        }
                                )
                                Text(
                                    text = "↺",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(currentTheme.text),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(BorderSubtle)
                                        .padding(horizontal = 2.dp)
                                        .clickable {
                                            if (videoAsset != null) viewModel.resetSpecialAssetTransform(videoAsset.id)
                                        }
                                )
                            }
                        }
                    }

                    // Right Scrollable Frame Ruler & Tracks
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // Ruler Row
                        Row(
                            modifier = Modifier
                                .height(22.dp)
                                .background(Color(currentTheme.bg))
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            uiState.project.frames.indices.forEach { idx ->
                                val isCurrent = idx == uiState.currentFrameIndex
                                val frame = uiState.project.frames.getOrNull(idx)
                                val hold = frame?.holdFrames ?: 1
                                val cellWidth = (36 * hold).coerceIn(36, 400).dp
                                Box(
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .fillMaxHeight()
                                        .clickable { viewModel.selectFrame(idx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCurrent) {
                                        // Playhead Needle Indicator
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(CyanAccent)
                                        )
                                    } else {
                                        Text(
                                            text = "${idx + 1}",
                                            fontSize = 9.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Drawing Layer Track
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(currentTheme.panel).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                uiState.project.frames.indices.forEach { idx ->
                                    val isCurrent = idx == uiState.currentFrameIndex
                                    val frame = uiState.project.frames.getOrNull(idx)
                                    val hold = frame?.holdFrames ?: 1
                                    val cellWidth = (36 * hold).coerceIn(36, 400).dp

                                    Box(
                                        modifier = Modifier
                                            .width(cellWidth)
                                            .fillMaxHeight()
                                            .padding(horizontal = 2.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isCurrent) Color(currentTheme.accent).copy(alpha = 0.45f)
                                                else BorderSubtle.copy(alpha = 0.3f)
                                            )
                                            .border(
                                                width = if (isCurrent) 1.5.dp else 0.5.dp,
                                                color = if (isCurrent) Color(currentTheme.accent) else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable { viewModel.selectFrame(idx) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Frame number or diamond
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            if (idx == 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(RoundedCornerShape(1.dp))
                                                        .background(NeonGreen)
                                                )
                                            } else {
                                                Text(
                                                    text = "${idx + 1}",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrent) Color(currentTheme.text) else TextMuted
                                                )
                                            }

                                            if (hold > 1) {
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "${hold}q",
                                                    fontSize = 8.sp,
                                                    color = Color(currentTheme.accent).copy(alpha = 0.85f),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Draggable Duration Handle ("bolinha" to stretch frame duration)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 2.dp)
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrent) Color(currentTheme.accent) else Color(currentTheme.text).copy(alpha = 0.6f))
                                                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                                .pointerInput(idx) {
                                                    var accum = 0f
                                                    detectHorizontalDragGestures(
                                                        onDragEnd = { accum = 0f }
                                                    ) { change, dragAmount ->
                                                        change.consume()
                                                        accum += dragAmount
                                                        val unitPx = 36.dp.toPx() * 0.45f
                                                        if (abs(accum) >= unitPx) {
                                                            val delta = (accum / unitPx).toInt()
                                                            if (delta != 0) {
                                                                val currHold = uiState.project.frames.getOrNull(idx)?.holdFrames ?: 1
                                                                val newHold = (currHold + delta).coerceIn(1, 60)
                                                                viewModel.setFrameHoldDuration(idx, newHold)
                                                                accum = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        // Audio Track Row
                        Row(
                            modifier = Modifier
                                .height(24.dp)
                                .fillMaxWidth()
                                .background(Color(currentTheme.bg).copy(alpha = 0.7f))
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (audioAsset != null) {
                                // Simulated waveform bars across frames
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    repeat(uiState.project.frames.size * 3) { bIdx ->
                                        val h = (8 + (bIdx * 7) % 12).dp
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(h)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(NeonGreen.copy(alpha = 0.7f))
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Nenhum arquivo de áudio carregado",
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // Video / GIF Track Row
                        Row(
                            modifier = Modifier
                                .height(24.dp)
                                .fillMaxWidth()
                                .background(Color(currentTheme.bg).copy(alpha = 0.85f))
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (videoAsset != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(OrangeAccent.copy(alpha = 0.35f))
                                        .border(1.dp, OrangeAccent, RoundedCornerShape(3.dp))
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "${videoAsset.name} (Opacidade: ${videoAsset.opacityPercent}%)",
                                        fontSize = 9.sp,
                                        color = OrangeAccent
                                    )
                                }
                            } else {
                                Text(
                                    text = "Toque em 'tr' para carregar vídeo ou GIF de referência",
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialogs & Overlays
        when (uiState.activeDialog) {
            EditorDialog.COLOR_PICKER -> {
                ColorPickerDialog(
                    initialColor = uiState.currentColor,
                    recentColors = uiState.recentColors,
                    onColorSelected = { newColor ->
                        viewModel.setColor(newColor)
                        viewModel.closeDialog()
                    },
                    onDismiss = { viewModel.closeDialog() }
                )
            }
            EditorDialog.PROJECT_SETTINGS -> {
                ProjectSettingsDialog(
                    project = uiState.project,
                    onUpdateProjectName = { viewModel.setProjectName(it) },
                    onUpdateResolution = { viewModel.setResolution(it) },
                    onUpdateFps = { viewModel.setFps(it) },
                    onUpdateOnionSkin = { viewModel.updateOnionSkinSettings(it) },
                    onClose = { viewModel.closeDialog() }
                )
            }
            EditorDialog.EXPORT -> {
                ExportDialog(
                    exportState = uiState.exportState,
                    onExportMp4 = { viewModel.exportToMp4(context) },
                    onExportGif = { viewModel.exportToGif(context) },
                    onExportSpriteSheet = { viewModel.exportToSpriteSheet(context) },
                    onExportPngFrame = { viewModel.exportCurrentFramePng(context) },
                    onExportFramePackZip = { viewModel.exportFramePackZip(context) },
                    onExportProjectFile = { viewModel.exportProjectFile(context) },
                    onDismissState = { viewModel.dismissExportState() },
                    onClose = {
                        viewModel.dismissExportState()
                        viewModel.closeDialog()
                    }
                )
            }
            else -> {}
        }

        // Quick Theme Selection Dialog
        if (showThemeMenu) {
            AlertDialog(
                onDismissRequest = { showThemeMenu = false },
                title = { Text("Escolher Tema da Interface") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        KodanimaTheme.values().forEach { th ->
                            val isSel = th == currentTheme
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(th.accent).copy(alpha = 0.25f) else Color(th.panel))
                                    .border(1.dp, if (isSel) Color(th.accent) else BorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.setTheme(th)
                                        showThemeMenu = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(th.bg))
                                            .border(1.5.dp, Color(th.accent), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = th.namePt,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = Color(th.text)
                                    )
                                }
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(th.accent))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeMenu = false }) { Text("Fechar") }
                }
            )
        }

        // Media Import Choice Modal
        if (showMediaImportChoice) {
            AlertDialog(
                onDismissRequest = { showMediaImportChoice = false },
                title = { Text("Importar Mídia para a Timeline") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showMediaImportChoice = false
                                audioPicker.launch("audio/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Faixa de Áudio (.mp3, .wav, .aac)")
                        }
                        Button(
                            onClick = {
                                showMediaImportChoice = false
                                videoPicker.launch("video/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VideoFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vídeo ou GIF de Referência")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMediaImportChoice = false }) { Text("Cancelar") }
                }
            )
        }

        // Onion Opacity Dialog
        if (showOnionOpacityDialog) {
            var sliderVal by remember { mutableStateOf(uiState.project.onionSkin.opacity) }
            AlertDialog(
                onDismissRequest = { showOnionOpacityDialog = false },
                title = { Text("Opacidade da Camada Cebola") },
                text = {
                    Column {
                        Text("Ajuste a intensidade das sombras de quadros anteriores:")
                        Spacer(modifier = Modifier.height(12.dp))
                        Slider(
                            value = sliderVal,
                            onValueChange = { sliderVal = it },
                            valueRange = 0.1f..1.0f
                        )
                        Text(
                            text = "${(sliderVal * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = Color(currentTheme.accent)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setOnionSkinOpacity(sliderVal)
                        showOnionOpacityDialog = false
                    }) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOnionOpacityDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // FPS Dialog
        if (showFpsDialog) {
            var selectedFps by remember { mutableStateOf(uiState.project.fps) }
            AlertDialog(
                onDismissRequest = { showFpsDialog = false },
                title = { Text("Taxa de Quadros por Segundo (FPS)") },
                text = {
                    Column {
                        listOf(12, 24, 30, 60).forEach { fps ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFps = fps }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$fps FPS",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedFps == fps) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedFps == fps) Color(currentTheme.accent) else Color(currentTheme.text)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setProjectFps(selectedFps)
                        showFpsDialog = false
                    }) {
                        Text("Confirmar")
                    }
                }
            )
        }
    }
}

@Composable
private fun DesktopToolButton(
    icon: ImageVector,
    tooltip: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) accentColor.copy(alpha = 0.35f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.dp,
            color = if (isSelected) accentColor else Color.Transparent
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = tooltip,
                tint = if (isSelected) accentColor else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TimelineIconButton(
    icon: ImageVector,
    tooltip: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = tooltip,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun TimelineTextButton(
    text: String,
    tooltip: String,
    textColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        color = BorderSubtle.copy(alpha = 0.35f)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
