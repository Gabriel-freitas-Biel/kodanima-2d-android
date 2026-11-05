package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppScreen
import com.example.model.AppUiMode
import com.example.model.ToolType
import com.example.ui.components.CanvasView
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.LayersBottomSheet
import com.example.ui.components.ProjectSettingsDialog
import com.example.ui.components.ProjectsListDialog
import com.example.ui.components.TimelineBar
import com.example.ui.components.ToolboxPalette
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.OrangeAccent
import com.example.viewmodel.EditorDialog
import com.example.viewmodel.KodanimaViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AnimationEditorScreen(
    viewModel: KodanimaViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentTheme = uiState.preferences.theme

    val bgColor = Color(currentTheme.bg)
    val panelColor = Color(currentTheme.panel)
    val accentColor = Color(currentTheme.accent)
    val textColor = Color(currentTheme.text)
    val borderCol = accentColor.copy(alpha = 0.35f)

    BackHandler {
        viewModel.saveCurrentProject(context)
        viewModel.navigateTo(AppScreen.HOME)
    }

    // File picker for .kodanima project files
    val projectFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonText = reader.readText()
                reader.close()
                inputStream?.close()
                viewModel.importProjectFromJson(jsonText)
                Toast.makeText(context, "Projeto carregado com sucesso!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar arquivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Media picker for reference images
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setReferenceImageUri(uri.toString())
            Toast.makeText(context, "Imagem de referência importada!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
        containerColor = bgColor,
        topBar = {
            // Ultra-slim, compact top bar that NEVER expands on zoom
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = panelColor,
                tonalElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Back button, Kodanima Icon and Project Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentProject(context)
                                viewModel.navigateTo(AppScreen.HOME)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar ao Início",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Official Kodanima Icon
                        Image(
                            painter = painterResource(R.drawable.ic_kodanima_k),
                            contentDescription = "Kodanima2D",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Text(
                            text = uiState.project.name,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier
                                .clickable { viewModel.openDialog(EditorDialog.PROJECT_SETTINGS) }
                                .padding(horizontal = 4.dp)
                        )

                        Text(
                            text = "${uiState.project.fps}FPS",
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }

                    // Center: Rotation indicator (if rotated)
                    val rotDeg = uiState.rotation.roundToInt()
                    if (abs(uiState.rotation) > 0.5f) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    viewModel.resetRotation()
                                    Toast.makeText(context, "Giro zerado (0°)", Toast.LENGTH_SHORT).show()
                                },
                            color = OrangeAccent.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeAccent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.RestartAlt,
                                    contentDescription = "Zerar giro",
                                    tint = OrangeAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${rotDeg}°",
                                    color = OrangeAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Right Actions: Clear, Projects, Save, PC Mode, Settings, Export
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        // Clear frame
                        IconButton(
                            onClick = { viewModel.clearCurrentFrame() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Limpar quadro atual",
                                tint = OrangeAccent.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Save
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentProject(context)
                                Toast.makeText(context, "Projeto Salvo!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Salvar Projeto",
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Switch to PC Mode
                        IconButton(
                            onClick = { viewModel.setUiMode(AppUiMode.PC) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.DesktopWindows,
                                contentDescription = "Modo PC Desktop",
                                tint = textColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Settings
                        IconButton(
                            onClick = { viewModel.openDialog(EditorDialog.PROJECT_SETTINGS) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Configurações",
                                tint = textColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Export (MP4, GIF, Frame Pack ZIP, PNG)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor)
                                .clickable { viewModel.openDialog(EditorDialog.EXPORT) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("top_bar_export_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = bgColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Exportar",
                                    color = bgColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            TimelineBar(
                frames = uiState.project.frames,
                currentFrameIndex = uiState.currentFrameIndex,
                isPlaying = uiState.isPlaying,
                onionSkinEnabled = uiState.onionSkinEnabled,
                layerCount = uiState.project.frames[uiState.currentFrameIndex].layers.size,
                fps = uiState.project.fps,
                onSelectFrame = { viewModel.goToFrame(it) },
                onTogglePlay = { viewModel.togglePlayback() },
                onNextFrame = { viewModel.nextFrame() },
                onPrevFrame = { viewModel.prevFrame() },
                onAddFrame = { viewModel.addFrame() },
                onDuplicateFrame = { viewModel.duplicateCurrentFrame() },
                onDeleteFrame = { viewModel.deleteCurrentFrame() },
                onToggleOnionSkin = { viewModel.toggleOnionSkin() },
                onOpenLayers = { viewModel.openDialog(EditorDialog.LAYERS) },
                onOpenSettings = { viewModel.openDialog(EditorDialog.PROJECT_SETTINGS) },
                theme = currentTheme
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Hardware-Accelerated Animation Canvas
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
                onTransformChange = { z, px, py, r ->
                    viewModel.updateTransform(z, px, py, r)
                },
                onStrokeCompleted = { stroke ->
                    viewModel.addStrokeToCurrentLayer(stroke)
                },
                onEyedropperColorPicked = { pickedColor ->
                    viewModel.setColor(pickedColor)
                    viewModel.selectTool(ToolType.PEN)
                },
                onBucketFill = { x, y ->
                    viewModel.performBucketFill(x, y)
                },
                onMagicWand = { x, y ->
                    viewModel.performMagicWand(x, y)
                },
                onLassoCompleted = { lassoPoints ->
                    viewModel.setLassoSelection(lassoPoints)
                }
            )

            // Floating Left Toolbox Palette (Thumb-friendly touch tools)
            ToolboxPalette(
                selectedTool = uiState.selectedTool,
                currentColor = uiState.currentColor,
                brushSize = uiState.brushSize,
                isStylusOnlyMode = uiState.isStylusOnlyMode,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onSelectTool = { viewModel.selectTool(it) },
                onChangeBrushSize = { viewModel.setBrushSize(it) },
                onOpenColorPicker = { viewModel.openDialog(EditorDialog.COLOR_PICKER) },
                onToggleStylusOnly = { viewModel.setStylusOnlyMode(!uiState.isStylusOnlyMode) },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onResetZoom = { viewModel.resetCanvasTransform() },
                theme = currentTheme,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            )
        }
    }

    // Modal Dialogs
    when (uiState.activeDialog) {
        EditorDialog.LAYERS -> {
            LayersBottomSheet(
                layers = uiState.project.frames[uiState.currentFrameIndex].layers,
                activeLayerIndex = uiState.activeLayerIndex,
                onSelectLayer = { viewModel.setActiveLayer(it) },
                onAddLayer = { viewModel.addLayer() },
                onDeleteActiveLayer = { viewModel.deleteActiveLayer() },
                onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                onToggleLock = { viewModel.toggleLayerLock(it) },
                onChangeOpacity = { idx, op -> viewModel.setLayerOpacity(idx, op) },
                onDismiss = { viewModel.closeDialog() }
            )
        }
        EditorDialog.COLOR_PICKER -> {
            ColorPickerDialog(
                initialColor = uiState.currentColor,
                recentColors = uiState.recentColors,
                onColorSelected = { viewModel.setColor(it) },
                onDismiss = { viewModel.closeDialog() }
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
        EditorDialog.PROJECTS_LIST -> {
            ProjectsListDialog(
                onNewProject = { name, res, fps ->
                    viewModel.loadNewEmptyProject(name, res, fps)
                },
                onLoadDemo = {
                    viewModel.loadDemoProject()
                },
                onPickProjectFile = {
                    projectFilePicker.launch(arrayOf("application/json", "*/*"))
                },
                onPickReferenceImage = {
                    imagePicker.launch("image/*")
                },
                onClose = { viewModel.closeDialog() }
            )
        }
        else -> { /* No dialog active */ }
    }
}
