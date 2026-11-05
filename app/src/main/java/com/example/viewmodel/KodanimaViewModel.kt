package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.FileImportExportManager
import com.example.engine.GifEncoder
import com.example.engine.KodanimaGraphicsEngine
import com.example.engine.ProjectStorageManager
import com.example.engine.SampleProjects
import com.example.model.AnimationProject
import com.example.model.AppPreferences
import com.example.model.AppUiMode
import com.example.model.DrawingStroke
import com.example.model.Frame
import com.example.model.Layer
import com.example.model.OnionSkinSettings
import com.example.model.PressureCurve
import com.example.model.ProjectResolution
import com.example.model.ReferenceImageState
import com.example.model.SelectionRegion
import com.example.model.StrokePoint
import com.example.model.TabletDriverSettings
import com.example.model.ToolType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

import com.example.model.AppScreen

enum class EditorDialog {
    NONE,
    PROJECT_SETTINGS,
    LAYERS,
    COLOR_PICKER,
    EXPORT,
    NEW_PROJECT,
    PROJECTS_LIST
}

sealed class ExportState {
    object Idle : ExportState()
    data class Progress(val message: String, val percent: Float) : ExportState()
    data class Success(val message: String, val filename: String) : ExportState()
    data class Error(val error: String) : ExportState()
}

data class EditorUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val preferences: AppPreferences = AppPreferences(),
    val savedProjects: List<AnimationProject> = emptyList(),
    val project: AnimationProject = SampleProjects.createBouncingBallProject(),
    val currentFrameIndex: Int = 0,
    val activeLayerIndex: Int = 1,
    val selectedTool: ToolType = ToolType.PEN,
    val currentColor: Long = 0xFF00D2FF, // Kodanima Cyan
    val brushSize: Float = 6f,
    val brushOpacity: Float = 1.0f,
    val isPlaying: Boolean = false,
    val isLooping: Boolean = true,
    val isStylusOnlyMode: Boolean = false,
    val onionSkinEnabled: Boolean = true,
    val activeDialog: EditorDialog = EditorDialog.NONE,
    val exportState: ExportState = ExportState.Idle,
    val referenceImage: ReferenceImageState = ReferenceImageState(),
    val zoom: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val rotation: Float = 0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val statusMessage: String? = null,
    val recentColors: List<Long> = listOf(
        0xFF00D2FF, 0xFFFF7A00, 0xFFFF3366, 0xFF00E676,
        0xFFFFFFFF, 0xFF12131A, 0xFFFFD700, 0xFF7C4DFF
    ),
    val activeSelection: com.example.model.SelectionRegion = com.example.model.SelectionRegion()
)

class KodanimaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoHistory = mutableListOf<Frame>()
    private val redoHistory = mutableListOf<Frame>()
    private val maxHistorySize = 30

    private var playbackJob: Job? = null
    private var appContext: Context? = null

    fun initializeWithContext(context: Context) {
        appContext = context.applicationContext
        val projects = ProjectStorageManager.loadAllProjects(context)
        _uiState.update { it.copy(savedProjects = projects) }
    }

    // --- Navigation Screens & Auto-Save ---

    fun navigateTo(screen: AppScreen) {
        if (_uiState.value.currentScreen == AppScreen.EDITOR && screen != AppScreen.EDITOR) {
            pausePlayback()
            appContext?.let { ctx ->
                saveCurrentProject(ctx)
            }
        }
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setUiMode(mode: AppUiMode) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(uiMode = mode))
        }
    }

    fun setTheme(theme: com.example.model.KodanimaTheme) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(theme = theme))
        }
    }

    fun setTimelineLayoutMode(mode: com.example.model.TimelineLayoutMode) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(timelineLayoutMode = mode))
        }
    }

    fun updateTabletSettings(settings: TabletDriverSettings) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(tabletSettings = settings))
        }
    }

    fun loadNewEmptyProject(name: String, resolution: ProjectResolution, fps: Int) {
        pausePlayback()
        undoHistory.clear()
        redoHistory.clear()
        val newProj = SampleProjects.createEmptyProject(name, resolution, fps)
        _uiState.update {
            it.copy(
                project = newProj,
                currentFrameIndex = 0,
                activeLayerIndex = 0,
                activeDialog = EditorDialog.NONE
            )
        }
        updateUndoRedoAvailability()
    }

    fun openProject(project: AnimationProject) {
        pausePlayback()
        undoHistory.clear()
        redoHistory.clear()
        val initialFrame = project.lastActiveFrameIndex.coerceIn(0, (project.frames.size - 1).coerceAtLeast(0))
        _uiState.update {
            it.copy(
                project = project,
                currentFrameIndex = initialFrame,
                activeLayerIndex = 0,
                currentScreen = AppScreen.EDITOR,
                activeDialog = EditorDialog.NONE
            )
        }
        updateUndoRedoAvailability()
    }

    fun createAndOpenNewProject(context: Context, name: String, resolution: ProjectResolution, fps: Int) {
        pausePlayback()
        undoHistory.clear()
        redoHistory.clear()
        val newProj = SampleProjects.createEmptyProject(name, resolution, fps)
        ProjectStorageManager.saveProject(context, newProj)
        val updatedList = ProjectStorageManager.loadAllProjects(context)
        _uiState.update {
            it.copy(
                project = newProj,
                savedProjects = updatedList,
                currentFrameIndex = 0,
                activeLayerIndex = 0,
                currentScreen = AppScreen.EDITOR,
                activeDialog = EditorDialog.NONE
            )
        }
        updateUndoRedoAvailability()
    }

    fun saveCurrentProject(context: Context) {
        val current = _uiState.value.project.copy(updatedAt = System.currentTimeMillis())
        ProjectStorageManager.saveProject(context, current)
        val updatedList = ProjectStorageManager.loadAllProjects(context)
        _uiState.update {
            it.copy(
                project = current,
                savedProjects = updatedList,
                statusMessage = "Projeto salvo com sucesso!"
            )
        }
    }

    fun deleteProject(context: Context, projectId: String) {
        ProjectStorageManager.deleteProject(context, projectId)
        val updated = ProjectStorageManager.loadAllProjects(context)
        _uiState.update { it.copy(savedProjects = updated) }
    }

    fun duplicateProject(context: Context, projectId: String) {
        ProjectStorageManager.duplicateProject(context, projectId)
        val updated = ProjectStorageManager.loadAllProjects(context)
        _uiState.update { it.copy(savedProjects = updated) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    // --- Tablet / Stylus Pressure Curve Mapping ---

    fun computeEffectivePressure(rawPressure: Float): Float {
        val curve = _uiState.value.preferences.tabletSettings.pressureCurve
        val clamped = rawPressure.coerceIn(0.01f, 1.0f)
        return clamped.pow(curve.exponent).coerceIn(0.15f, 2.0f)
    }

    // --- Drawing & Tool Selection ---

    fun selectTool(tool: ToolType) {
        _uiState.update { it.copy(selectedTool = tool) }
    }

    fun setColor(color: Long) {
        _uiState.update { state ->
            val updatedRecents = if (!state.recentColors.contains(color)) {
                listOf(color) + state.recentColors.take(7)
            } else {
                state.recentColors
            }
            state.copy(currentColor = color, recentColors = updatedRecents)
        }
    }

    fun setBrushSize(size: Float) {
        _uiState.update { it.copy(brushSize = size.coerceIn(1f, 100f)) }
    }

    fun setBrushOpacity(opacity: Float) {
        _uiState.update { it.copy(brushOpacity = opacity.coerceIn(0.05f, 1.0f)) }
    }

    fun setStylusOnlyMode(enabled: Boolean) {
        _uiState.update { it.copy(isStylusOnlyMode = enabled) }
    }

    fun toggleOnionSkin() {
        _uiState.update {
            val updatedProject = it.project.copy(
                onionSkin = it.project.onionSkin.copy(isEnabled = !it.onionSkinEnabled)
            )
            it.copy(
                onionSkinEnabled = !it.onionSkinEnabled,
                project = updatedProject
            )
        }
    }

    fun updateOnionSkinSettings(settings: OnionSkinSettings) {
        _uiState.update {
            it.copy(
                onionSkinEnabled = settings.isEnabled,
                project = it.project.copy(onionSkin = settings)
            )
        }
    }

    fun openDialog(dialog: EditorDialog) {
        _uiState.update { it.copy(activeDialog = dialog) }
    }

    fun closeDialog() {
        _uiState.update { it.copy(activeDialog = EditorDialog.NONE) }
    }

    // --- Keyboard Shortcut Handler (PC Mode & Physical Keyboard) ---

    fun handlePhysicalKeyEvent(event: KeyEvent, context: Context): Boolean {
        if (!_uiState.value.preferences.tabletSettings.keyboardShortcutsEnabled) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val isCtrl = event.isCtrlPressed

        when (event.keyCode) {
            KeyEvent.KEYCODE_B -> { selectTool(ToolType.PEN); return true }
            KeyEvent.KEYCODE_P -> { selectTool(ToolType.PENCIL); return true }
            KeyEvent.KEYCODE_E -> { selectTool(ToolType.ERASER); return true }
            KeyEvent.KEYCODE_G -> { selectTool(ToolType.BUCKET); return true }
            KeyEvent.KEYCODE_I -> { selectTool(ToolType.EYEDROPPER); return true }
            KeyEvent.KEYCODE_L -> { selectTool(ToolType.LINE); return true }
            KeyEvent.KEYCODE_U -> { selectTool(ToolType.RECTANGLE); return true }
            KeyEvent.KEYCODE_C -> { selectTool(ToolType.CIRCLE); return true }
            KeyEvent.KEYCODE_O -> { toggleOnionSkin(); return true }
            KeyEvent.KEYCODE_SPACE -> { togglePlayback(); return true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { prevFrame(); return true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { nextFrame(); return true }
            KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.KEYCODE_DEL -> {
                if (isCtrl) { clearCurrentFrame(); return true }
            }
            KeyEvent.KEYCODE_Z -> {
                if (isCtrl) {
                    if (event.isShiftPressed) redo() else undo()
                    return true
                }
            }
            KeyEvent.KEYCODE_Y -> {
                if (isCtrl) { redo(); return true }
            }
            KeyEvent.KEYCODE_S -> {
                if (isCtrl) {
                    saveCurrentProject(context)
                    return true
                }
            }
            KeyEvent.KEYCODE_N -> {
                if (isCtrl) {
                    addFrame()
                    return true
                }
            }
        }
        return false
    }

    // --- Frame & Layer Manipulation ---

    fun goToFrame(index: Int) {
        val total = _uiState.value.project.frames.size
        if (total == 0) return
        val clamped = index.coerceIn(0, total - 1)
        if (clamped != _uiState.value.currentFrameIndex) {
            undoHistory.clear()
            redoHistory.clear()
            _uiState.update {
                it.copy(
                    currentFrameIndex = clamped,
                    project = it.project.copy(lastActiveFrameIndex = clamped, updatedAt = System.currentTimeMillis())
                )
            }
            updateUndoRedoAvailability()
        }
    }

    fun setFrameHoldDuration(frameIndex: Int, holdCount: Int) {
        val clampedHold = holdCount.coerceIn(1, 120)
        _uiState.update { state ->
            val updated = state.project.frames.toMutableList()
            if (frameIndex in updated.indices) {
                updated[frameIndex] = updated[frameIndex].copy(holdFrames = clampedHold)
                state.copy(
                    project = state.project.copy(frames = updated, updatedAt = System.currentTimeMillis())
                )
            } else state
        }
    }

    fun performBucketFill(x: Float, y: Float) {
        val proj = _uiState.value.project
        val currFrame = proj.frames.getOrNull(_uiState.value.currentFrameIndex) ?: return
        val bmp = KodanimaGraphicsEngine.renderFrameToBitmap(
            currFrame,
            proj.resolution.width,
            proj.resolution.height,
            android.graphics.Color.WHITE
        )
        val px = x.toInt().coerceIn(0, bmp.width - 1)
        val py = y.toInt().coerceIn(0, bmp.height - 1)
        val spans = KodanimaGraphicsEngine.createFloodFillSpans(bmp, px, py)
        bmp.recycle()

        if (spans.isNotEmpty()) {
            val stroke = DrawingStroke(
                tool = ToolType.BUCKET,
                color = _uiState.value.currentColor,
                brushSize = 2.5f,
                opacity = _uiState.value.brushOpacity,
                points = spans,
                isFilled = true
            )
            addStrokeToCurrentLayer(stroke)
        }
    }

    fun performMagicWand(x: Float, y: Float) {
        val proj = _uiState.value.project
        val currFrame = proj.frames.getOrNull(_uiState.value.currentFrameIndex) ?: return
        val bmp = KodanimaGraphicsEngine.renderFrameToBitmap(
            currFrame,
            proj.resolution.width,
            proj.resolution.height,
            android.graphics.Color.WHITE
        )
        val px = x.toInt().coerceIn(0, bmp.width - 1)
        val py = y.toInt().coerceIn(0, bmp.height - 1)
        val boundary = KodanimaGraphicsEngine.createMagicWandBoundary(bmp, px, py)
        bmp.recycle()

        _uiState.update {
            it.copy(
                activeSelection = com.example.model.SelectionRegion(
                    isActive = boundary.isNotEmpty(),
                    points = boundary
                )
            )
        }
    }

    fun setLassoSelection(points: List<StrokePoint>) {
        val closed = if (points.isNotEmpty() && points.first() != points.last()) points + points.first() else points
        _uiState.update {
            it.copy(
                activeSelection = com.example.model.SelectionRegion(
                    isActive = closed.isNotEmpty(),
                    points = closed
                )
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(activeSelection = com.example.model.SelectionRegion(isActive = false, points = emptyList()))
        }
    }

    fun nextFrame() {
        val nextIdx = (_uiState.value.currentFrameIndex + 1) % _uiState.value.project.frames.size
        goToFrame(nextIdx)
    }

    fun prevFrame() {
        val total = _uiState.value.project.frames.size
        val prevIdx = if (_uiState.value.currentFrameIndex - 1 < 0) total - 1 else _uiState.value.currentFrameIndex - 1
        goToFrame(prevIdx)
    }

    fun addFrame() {
        pushCurrentFrameToUndo()
        _uiState.update { state ->
            val newFrame = Frame(layers = listOf(Layer(name = "Camada 1")))
            val insertAt = state.currentFrameIndex + 1
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames.add(insertAt, newFrame)
            val updatedProject = state.project.copy(frames = updatedFrames, updatedAt = System.currentTimeMillis())
            state.copy(project = updatedProject, currentFrameIndex = insertAt, activeLayerIndex = 0)
        }
        undoHistory.clear()
        redoHistory.clear()
        updateUndoRedoAvailability()
    }

    fun duplicateCurrentFrame() {
        _uiState.update { state ->
            val curr = state.project.frames[state.currentFrameIndex]
            val duplicatedLayers = curr.layers.map { layer ->
                layer.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    strokes = layer.strokes.map { it.copy(id = java.util.UUID.randomUUID().toString()) }
                )
            }
            val duplicatedFrame = Frame(
                id = java.util.UUID.randomUUID().toString(),
                layers = duplicatedLayers,
                holdFrames = curr.holdFrames
            )
            val insertAt = state.currentFrameIndex + 1
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames.add(insertAt, duplicatedFrame)
            state.copy(
                project = state.project.copy(frames = updatedFrames, updatedAt = System.currentTimeMillis()),
                currentFrameIndex = insertAt
            )
        }
        undoHistory.clear()
        redoHistory.clear()
        updateUndoRedoAvailability()
    }

    fun deleteCurrentFrame() {
        if (_uiState.value.project.frames.size <= 1) return
        _uiState.update { state ->
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames.removeAt(state.currentFrameIndex)
            val newIndex = min(state.currentFrameIndex, updatedFrames.size - 1)
            state.copy(
                project = state.project.copy(frames = updatedFrames, updatedAt = System.currentTimeMillis()),
                currentFrameIndex = newIndex
            )
        }
        undoHistory.clear()
        redoHistory.clear()
        updateUndoRedoAvailability()
    }

    fun clearCurrentFrame() {
        pushCurrentFrameToUndo()
        _uiState.update { state ->
            val currFrame = state.project.frames[state.currentFrameIndex]
            val clearedLayers = currFrame.layers.map { it.copy(strokes = emptyList()) }
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames[state.currentFrameIndex] = currFrame.copy(layers = clearedLayers)
            state.copy(project = state.project.copy(frames = updatedFrames))
        }
        updateUndoRedoAvailability()
    }

    fun addStrokeToCurrentLayer(stroke: DrawingStroke) {
        pushCurrentFrameToUndo()
        redoHistory.clear()

        _uiState.update { state ->
            val frameIndex = state.currentFrameIndex
            val layerIndex = state.activeLayerIndex
            val currFrame = state.project.frames[frameIndex]

            val validLayerIndex = layerIndex.coerceIn(0, currFrame.layers.size - 1)
            val targetLayer = currFrame.layers[validLayerIndex]

            if (targetLayer.isLocked || !targetLayer.isVisible) {
                return@update state
            }

            val updatedStrokes = targetLayer.strokes + stroke
            val updatedLayers = currFrame.layers.toMutableList()
            updatedLayers[validLayerIndex] = targetLayer.copy(strokes = updatedStrokes)

            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames[frameIndex] = currFrame.copy(layers = updatedLayers)

            state.copy(project = state.project.copy(frames = updatedFrames, updatedAt = System.currentTimeMillis()))
        }
        updateUndoRedoAvailability()
    }

    // --- Layers Management ---

    fun setActiveLayer(index: Int) {
        val currFrame = _uiState.value.project.frames[_uiState.value.currentFrameIndex]
        val clamped = index.coerceIn(0, currFrame.layers.size - 1)
        _uiState.update { it.copy(activeLayerIndex = clamped) }
    }

    fun addLayer() {
        pushCurrentFrameToUndo()
        _uiState.update { state ->
            val currFrame = state.project.frames[state.currentFrameIndex]
            val newLayer = Layer(name = "Camada ${currFrame.layers.size + 1}")
            val updatedLayers = currFrame.layers + newLayer
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames[state.currentFrameIndex] = currFrame.copy(layers = updatedLayers)
            state.copy(
                project = state.project.copy(frames = updatedFrames),
                activeLayerIndex = updatedLayers.size - 1
            )
        }
        updateUndoRedoAvailability()
    }

    fun deleteActiveLayer() {
        val currFrame = _uiState.value.project.frames[_uiState.value.currentFrameIndex]
        if (currFrame.layers.size <= 1) return
        pushCurrentFrameToUndo()
        _uiState.update { state ->
            val updatedLayers = currFrame.layers.toMutableList()
            updatedLayers.removeAt(state.activeLayerIndex)
            val newActiveIndex = min(state.activeLayerIndex, updatedLayers.size - 1)
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames[state.currentFrameIndex] = currFrame.copy(layers = updatedLayers)
            state.copy(
                project = state.project.copy(frames = updatedFrames),
                activeLayerIndex = newActiveIndex
            )
        }
        updateUndoRedoAvailability()
    }

    fun toggleLayerVisibility(layerIndex: Int) {
        _uiState.update { state ->
            val currFrame = state.project.frames[state.currentFrameIndex]
            val updatedLayers = currFrame.layers.toMutableList()
            val l = updatedLayers[layerIndex]
            updatedLayers[layerIndex] = l.copy(isVisible = !l.isVisible)
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames[state.currentFrameIndex] = currFrame.copy(layers = updatedLayers)
            state.copy(project = state.project.copy(frames = updatedFrames))
        }
    }

    fun toggleLayerLock(layerIndex: Int) {
        _uiState.update { state ->
            val currFrame = state.project.frames[state.currentFrameIndex]
            val updatedLayers = currFrame.layers.toMutableList()
            val l = updatedLayers[layerIndex]
            updatedLayers[layerIndex] = l.copy(isLocked = !l.isLocked)
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames[state.currentFrameIndex] = currFrame.copy(layers = updatedLayers)
            state.copy(project = state.project.copy(frames = updatedFrames))
        }
    }

    fun setLayerOpacity(layerIndex: Int, opacity: Float) {
        _uiState.update { state ->
            val currFrame = state.project.frames[state.currentFrameIndex]
            val updatedLayers = currFrame.layers.toMutableList()
            val l = updatedLayers[layerIndex]
            updatedLayers[layerIndex] = l.copy(opacity = opacity.coerceIn(0f, 1f))
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames[state.currentFrameIndex] = currFrame.copy(layers = updatedLayers)
            state.copy(project = state.project.copy(frames = updatedFrames))
        }
    }

    // --- Undo / Redo ---

    private fun pushCurrentFrameToUndo() {
        val curr = _uiState.value.project.frames[_uiState.value.currentFrameIndex]
        undoHistory.add(curr)
        if (undoHistory.size > maxHistorySize) {
            undoHistory.removeAt(0)
        }
    }

    fun undo() {
        if (undoHistory.isEmpty()) return
        val previousState = undoHistory.removeAt(undoHistory.size - 1)
        val currentFrame = _uiState.value.project.frames[_uiState.value.currentFrameIndex]
        redoHistory.add(currentFrame)

        _uiState.update { state ->
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames[state.currentFrameIndex] = previousState
            state.copy(project = state.project.copy(frames = updatedFrames))
        }
        updateUndoRedoAvailability()
    }

    fun redo() {
        if (redoHistory.isEmpty()) return
        val nextState = redoHistory.removeAt(redoHistory.size - 1)
        val currentFrame = _uiState.value.project.frames[_uiState.value.currentFrameIndex]
        undoHistory.add(currentFrame)

        _uiState.update { state ->
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames[state.currentFrameIndex] = nextState
            state.copy(project = state.project.copy(frames = updatedFrames))
        }
        updateUndoRedoAvailability()
    }

    private fun updateUndoRedoAvailability() {
        _uiState.update {
            it.copy(canUndo = undoHistory.isNotEmpty(), canRedo = redoHistory.isNotEmpty())
        }
    }

    // --- Playback & Animation Loop ---

    fun togglePlayback() {
        if (_uiState.value.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        playbackJob?.cancel()
        _uiState.update { it.copy(isPlaying = true) }
        playbackJob = viewModelScope.launch {
            while (isActive && _uiState.value.isPlaying) {
                val fps = max(1, _uiState.value.project.fps)
                val delayTime = (1000L / fps)
                delay(delayTime)

                _uiState.update { state ->
                    val total = state.project.frames.size
                    if (total <= 1) return@update state
                    val next = (state.currentFrameIndex + 1) % total
                    state.copy(currentFrameIndex = next)
                }
            }
        }
    }

    fun pausePlayback() {
        playbackJob?.cancel()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun toggleLoop() {
        _uiState.update { it.copy(isLooping = !it.isLooping) }
    }

    fun setFps(fps: Int) {
        val clamped = fps.coerceIn(1, 60)
        _uiState.update { it.copy(project = it.project.copy(fps = clamped)) }
    }

    fun setResolution(res: ProjectResolution) {
        _uiState.update { it.copy(project = it.project.copy(resolution = res)) }
    }

    fun setProjectName(name: String) {
        _uiState.update { it.copy(project = it.project.copy(name = name.ifBlank { "Sem Título" })) }
    }

    // --- Canvas Zoom & Pan ---

    fun updateTransform(zoomChange: Float, panChangeX: Float, panChangeY: Float, rotationChange: Float = 0f) {
        _uiState.update {
            val newZoom = (it.zoom * zoomChange).coerceIn(0.2f, 8.0f)
            it.copy(
                zoom = newZoom,
                panX = it.panX + panChangeX,
                panY = it.panY + panChangeY,
                rotation = (it.rotation + rotationChange) % 360f
            )
        }
    }

    fun resetCanvasTransform() {
        _uiState.update { it.copy(zoom = 1.0f, panX = 0f, panY = 0f, rotation = 0f) }
    }

    fun resetRotation() {
        _uiState.update { it.copy(rotation = 0f) }
    }

    // --- Reference Image ---

    fun setReferenceImageUri(uriString: String) {
        _uiState.update {
            it.copy(
                referenceImage = it.referenceImage.copy(
                    uriString = uriString,
                    isVisible = true,
                    opacity = 0.5f
                )
            )
        }
    }

    fun clearReferenceImage() {
        _uiState.update { it.copy(referenceImage = ReferenceImageState()) }
    }

    // --- Import / Demo ---

    fun loadDemoProject() {
        pausePlayback()
        undoHistory.clear()
        redoHistory.clear()
        _uiState.update {
            it.copy(
                project = SampleProjects.createBouncingBallProject(),
                currentFrameIndex = 0,
                activeLayerIndex = 1,
                currentScreen = AppScreen.EDITOR,
                activeDialog = EditorDialog.NONE
            )
        }
        updateUndoRedoAvailability()
    }

    fun importProjectFromJson(jsonString: String) {
        try {
            val parsed = FileImportExportManager.projectFromJson(jsonString)
            pausePlayback()
            undoHistory.clear()
            redoHistory.clear()
            _uiState.update {
                it.copy(
                    project = parsed,
                    currentFrameIndex = 0,
                    activeLayerIndex = 0,
                    currentScreen = AppScreen.EDITOR,
                    activeDialog = EditorDialog.NONE
                )
            }
            updateUndoRedoAvailability()
        } catch (e: Exception) {
            _uiState.update { it.copy(exportState = ExportState.Error("Erro ao importar projeto: ${e.localizedMessage}")) }
        }
    }

    // --- Exporting ---

    fun exportToGif(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(exportState = ExportState.Progress("Renderizando quadros com motor Vulkan/OpenGL...", 0.2f)) }
            try {
                val proj = _uiState.value.project
                val w = min(640, proj.resolution.width)
                val h = (w * proj.resolution.height) / proj.resolution.width

                val bitmaps = mutableListOf<Bitmap>()
                val total = proj.frames.size

                for (i in proj.frames.indices) {
                    val frame = proj.frames[i]
                    val bmp = KodanimaGraphicsEngine.renderFrameToBitmap(frame, w, h, android.graphics.Color.WHITE)
                    repeat(max(1, frame.holdFrames)) {
                        bitmaps.add(bmp)
                    }
                    _uiState.update {
                        it.copy(exportState = ExportState.Progress("Renderizando quadros: ${i + 1}/$total", 0.2f + 0.4f * (i / total.toFloat())))
                    }
                }

                _uiState.update { it.copy(exportState = ExportState.Progress("Codificando arquivo GIF...", 0.7f)) }
                val encoder = GifEncoder()
                val gifBytes = encoder.encode(bitmaps, proj.fps)

                for (b in bitmaps) {
                    b.recycle()
                }

                val filename = "${proj.name.replace(" ", "_")}_kodanima.gif"
                FileImportExportManager.shareFile(context, filename, "image/gif", gifBytes)

                _uiState.update { it.copy(exportState = ExportState.Success("GIF exportado com sucesso!", filename)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportState = ExportState.Error("Falha na exportação GIF: ${e.localizedMessage}")) }
            }
        }
    }

    fun exportToSpriteSheet(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(exportState = ExportState.Progress("Gerando Sprite Sheet...", 0.3f)) }
            try {
                val proj = _uiState.value.project
                val cellW = min(320, proj.resolution.width)
                val cellH = (cellW * proj.resolution.height) / proj.resolution.width

                val sheetBmp = KodanimaGraphicsEngine.generateSpriteSheet(proj.frames, cellW, cellH, columns = 4)
                val out = ByteArrayOutputStream()
                sheetBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                sheetBmp.recycle()

                val filename = "${proj.name.replace(" ", "_")}_spritesheet.png"
                FileImportExportManager.shareFile(context, filename, "image/png", out.toByteArray())

                _uiState.update { it.copy(exportState = ExportState.Success("Sprite Sheet exportada com sucesso!", filename)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportState = ExportState.Error("Falha ao exportar Sprite Sheet: ${e.localizedMessage}")) }
            }
        }
    }

    fun exportCurrentFramePng(context: Context) {
        viewModelScope.launch {
            try {
                val proj = _uiState.value.project
                val curr = proj.frames[_uiState.value.currentFrameIndex]
                val bmp = KodanimaGraphicsEngine.renderFrameToBitmap(
                    curr,
                    proj.resolution.width,
                    proj.resolution.height,
                    android.graphics.Color.WHITE
                )
                val out = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                bmp.recycle()

                val filename = "${proj.name.replace(" ", "_")}_frame_${_uiState.value.currentFrameIndex + 1}.png"
                FileImportExportManager.shareFile(context, filename, "image/png", out.toByteArray())

                _uiState.update { it.copy(exportState = ExportState.Success("Quadro atual exportado em PNG!", filename)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportState = ExportState.Error("Falha ao exportar PNG: ${e.localizedMessage}")) }
            }
        }
    }

    fun exportToMp4(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(exportState = ExportState.Progress("Iniciando codificador nativo H.264...", 0.05f)) }
            try {
                val proj = _uiState.value.project
                val file = com.example.engine.AndroidVideoRenderer.renderToMp4(context, proj) { progress, message ->
                    _uiState.update { it.copy(exportState = ExportState.Progress(message, progress)) }
                }
                val filename = file.name
                val bytes = file.readBytes()
                FileImportExportManager.shareFile(context, filename, "video/mp4", bytes)
                _uiState.update { it.copy(exportState = ExportState.Success("Vídeo MP4 exportado com sucesso via aceleração de hardware!", filename)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportState = ExportState.Error("Falha na exportação MP4: ${e.localizedMessage}")) }
            }
        }
    }

    fun exportFramePackZip(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(exportState = ExportState.Progress("Gerando pacote de quadros PNG...", 0.05f)) }
            try {
                val proj = _uiState.value.project
                val file = com.example.engine.AndroidVideoRenderer.exportFramePackZip(context, proj) { progress, message ->
                    _uiState.update { it.copy(exportState = ExportState.Progress(message, progress)) }
                }
                val filename = file.name
                val bytes = file.readBytes()
                FileImportExportManager.shareFile(context, filename, "application/zip", bytes)
                _uiState.update { it.copy(exportState = ExportState.Success("Pacote de quadros PNG exportado em ZIP!", filename)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportState = ExportState.Error("Falha ao exportar pacote ZIP: ${e.localizedMessage}")) }
            }
        }
    }

    // --- Special Media Tracks (Audio & Video/GIF) ---

    fun addAudioTrack(name: String, uriString: String, durationSec: Double = 0.0) {
        val calculatedDuration = if (durationSec > 0.0) durationSec else (_uiState.value.project.frames.size.toDouble() / _uiState.value.project.fps)
        val asset = com.example.model.SpecialMediaAsset(
            name = name,
            uriString = uriString,
            type = "audio",
            startSeconds = 0.0,
            durationSeconds = calculatedDuration,
            audioVolumePercent = 100,
            waveformPeaks = listOf(0.15f, 0.45f, 0.8f, 0.95f, 0.6f, 0.85f, 0.5f, 0.75f, 0.9f, 0.35f, 0.7f, 0.5f)
        )
        _uiState.update {
            it.copy(project = it.project.copy(mediaAssets = it.project.mediaAssets + asset))
        }
    }

    fun addVideoTrack(name: String, uriString: String, durationSec: Double = 0.0) {
        val calculatedDuration = if (durationSec > 0.0) durationSec else (_uiState.value.project.frames.size.toDouble() / _uiState.value.project.fps)
        val asset = com.example.model.SpecialMediaAsset(
            name = name,
            uriString = uriString,
            type = "video",
            startSeconds = 0.0,
            durationSeconds = calculatedDuration,
            opacityPercent = 50,
            scalePercent = 100.0
        )
        _uiState.update {
            it.copy(project = it.project.copy(mediaAssets = it.project.mediaAssets + asset))
        }
    }

    fun toggleAudioMute(assetId: String) {
        _uiState.update { state ->
            val updated = state.project.mediaAssets.map {
                if (it.id == assetId) it.copy(audioMuted = !it.audioMuted) else it
            }
            state.copy(project = state.project.copy(mediaAssets = updated))
        }
    }

    fun setAudioVolume(assetId: String, volume: Int) {
        _uiState.update { state ->
            val updated = state.project.mediaAssets.map {
                if (it.id == assetId) it.copy(audioVolumePercent = volume.coerceIn(0, 100)) else it
            }
            state.copy(project = state.project.copy(mediaAssets = updated))
        }
    }

    fun setVideoOpacity(assetId: String, opacity: Int) {
        _uiState.update { state ->
            val updated = state.project.mediaAssets.map {
                if (it.id == assetId) it.copy(opacityPercent = opacity.coerceIn(0, 100)) else it
            }
            state.copy(project = state.project.copy(mediaAssets = updated))
        }
    }

    fun resetVideoTransform(assetId: String) {
        _uiState.update { state ->
            val updated = state.project.mediaAssets.map {
                if (it.id == assetId) it.copy(scalePercent = 100.0, rotationDegrees = 0.0) else it
            }
            state.copy(project = state.project.copy(mediaAssets = updated))
        }
    }

    fun selectLayer(index: Int) = setActiveLayer(index)
    fun deleteLayer(index: Int) = deleteActiveLayer()

    fun moveLayerUp(layerIndex: Int) {
        val currFrame = _uiState.value.project.frames.getOrNull(_uiState.value.currentFrameIndex) ?: return
        if (layerIndex >= currFrame.layers.size - 1) return
        pushCurrentFrameToUndo()
        _uiState.update { state ->
            val updatedLayers = currFrame.layers.toMutableList()
            val temp = updatedLayers[layerIndex]
            updatedLayers[layerIndex] = updatedLayers[layerIndex + 1]
            updatedLayers[layerIndex + 1] = temp
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames[state.currentFrameIndex] = currFrame.copy(layers = updatedLayers)
            state.copy(
                project = state.project.copy(frames = updatedFrames),
                activeLayerIndex = layerIndex + 1
            )
        }
    }

    fun moveLayerDown(layerIndex: Int) {
        val currFrame = _uiState.value.project.frames.getOrNull(_uiState.value.currentFrameIndex) ?: return
        if (layerIndex <= 0) return
        pushCurrentFrameToUndo()
        _uiState.update { state ->
            val updatedLayers = currFrame.layers.toMutableList()
            val temp = updatedLayers[layerIndex]
            updatedLayers[layerIndex] = updatedLayers[layerIndex - 1]
            updatedLayers[layerIndex - 1] = temp
            val updatedFrames = state.project.frames.toMutableList()
            updatedFrames[state.currentFrameIndex] = currFrame.copy(layers = updatedLayers)
            state.copy(
                project = state.project.copy(frames = updatedFrames),
                activeLayerIndex = layerIndex - 1
            )
        }
    }

    fun selectFrame(index: Int) = goToFrame(index)
    fun addNewFrame() = addFrame()

    fun insertFrameAtCurrentPosition() {
        pushCurrentFrameToUndo()
        _uiState.update { state ->
            val currFrame = state.project.frames[state.currentFrameIndex]
            val newFrame = Frame(layers = currFrame.layers.map { it.copy(strokes = emptyList()) })
            val updated = state.project.frames.toMutableList()
            val insertIdx = (state.currentFrameIndex + 1).coerceAtMost(updated.size)
            updated.add(insertIdx, newFrame)
            state.copy(
                project = state.project.copy(frames = updated, updatedAt = System.currentTimeMillis()),
                currentFrameIndex = insertIdx
            )
        }
    }

    fun setProjectFps(fps: Int) {
        _uiState.update { it.copy(project = it.project.copy(fps = fps.coerceIn(1, 120))) }
    }

    fun setProjectResolution(resolution: ProjectResolution) {
        _uiState.update { it.copy(project = it.project.copy(resolution = resolution)) }
    }

    fun setOnionSkinOpacity(opacity: Float) {
        _uiState.update {
            it.copy(project = it.project.copy(onionSkin = it.project.onionSkin.copy(opacity = opacity)))
        }
    }

    fun setOnionSkinSettings(settings: OnionSkinSettings) {
        _uiState.update {
            it.copy(project = it.project.copy(onionSkin = settings))
        }
    }

    fun importAudioUri(uriString: String) {
        val asset = com.example.model.SpecialMediaAsset(
            name = "Áudio da Timeline",
            uriString = uriString,
            type = "audio"
        )
        _uiState.update {
            it.copy(project = it.project.copy(mediaAssets = it.project.mediaAssets + asset))
        }
    }

    fun exportSingleFrame(context: Context, frameIndex: Int) {
        viewModelScope.launch {
            try {
                val proj = _uiState.value.project
                val clamped = frameIndex.coerceIn(0, proj.frames.size - 1)
                val curr = proj.frames[clamped]
                val bmp = KodanimaGraphicsEngine.renderFrameToBitmap(
                    curr,
                    proj.resolution.width,
                    proj.resolution.height,
                    android.graphics.Color.WHITE
                )
                val out = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                bmp.recycle()

                val filename = "${proj.name.replace(" ", "_")}_quadro_${clamped + 1}.png"
                FileImportExportManager.shareFile(context, filename, "image/png", out.toByteArray())

                _uiState.update { it.copy(exportState = ExportState.Success("Quadro ${clamped + 1} exportado em PNG!", filename)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportState = ExportState.Error("Falha ao exportar quadro: ${e.localizedMessage}")) }
            }
        }
    }

    fun exportProjectToJson(context: Context) = exportProjectFile(context)

    fun updateSpecialAssetScale(assetId: String, scalePercent: Double) {
        _uiState.update { state ->
            val updated = state.project.mediaAssets.map {
                if (it.id == assetId) it.copy(scalePercent = scalePercent) else it
            }
            state.copy(project = state.project.copy(mediaAssets = updated))
        }
    }

    fun updateSpecialAssetOpacity(assetId: String, opacity: Double) {
        _uiState.update { state ->
            val updated = state.project.mediaAssets.map {
                if (it.id == assetId) it.copy(opacityPercent = (opacity * 100).toInt()) else it
            }
            state.copy(project = state.project.copy(mediaAssets = updated))
        }
    }

    fun resetSpecialAssetTransform(assetId: String) {
        _uiState.update { state ->
            val updated = state.project.mediaAssets.map {
                if (it.id == assetId) it.copy(scalePercent = 100.0, rotationDegrees = 0.0) else it
            }
            state.copy(project = state.project.copy(mediaAssets = updated))
        }
    }

    fun deleteMediaAsset(assetId: String) {
        _uiState.update { state ->
            val updated = state.project.mediaAssets.filter { it.id != assetId }
            state.copy(project = state.project.copy(mediaAssets = updated))
        }
    }

    fun exportProjectFile(context: Context) {
        viewModelScope.launch {
            try {
                val proj = _uiState.value.project
                val jsonString = FileImportExportManager.projectToJson(proj)
                val filename = "${proj.name.replace(" ", "_")}.kodanima"
                FileImportExportManager.shareFile(context, filename, "application/json", jsonString.toByteArray(Charsets.UTF_8))

                _uiState.update { it.copy(exportState = ExportState.Success("Projeto Kodanima exportado!", filename)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportState = ExportState.Error("Falha ao exportar projeto: ${e.localizedMessage}")) }
            }
        }
    }

    fun dismissExportState() {
        _uiState.update { it.copy(exportState = ExportState.Idle) }
    }
}
