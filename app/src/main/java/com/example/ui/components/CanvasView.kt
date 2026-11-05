package com.example.ui.components

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.engine.KodanimaGraphicsEngine
import com.example.model.AnimationProject
import com.example.model.DrawingStroke
import com.example.model.StrokePoint
import com.example.model.ToolType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * High-performance hardware-accelerated animation canvas for Kodanima 2D.
 *
 * Touch & Stylus Gesture Handling:
 * - 1 finger / stylus: Precision drawing on active layer with pressure sensitivity.
 * - 2 fingers: Smooth pinch-to-zoom, two-finger pan, and real-time canvas rotation.
 * - Auto-cancels accidental strokes when a second finger touches down.
 * - Stylus-only palm rejection support.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CanvasView(
    project: AnimationProject,
    currentFrameIndex: Int,
    selectedTool: ToolType,
    currentColor: Long,
    brushSize: Float,
    brushOpacity: Float,
    onionSkinEnabled: Boolean,
    isStylusOnlyMode: Boolean,
    zoom: Float,
    panX: Float,
    panY: Float,
    rotation: Float,
    onTransformChange: (zoomChange: Float, panX: Float, panY: Float, rotationChange: Float) -> Unit,
    onStrokeCompleted: (DrawingStroke) -> Unit,
    onEyedropperColorPicked: (Long) -> Unit,
    activeSelection: com.example.model.SelectionRegion = com.example.model.SelectionRegion(),
    onBucketFill: ((Float, Float) -> Unit)? = null,
    onMagicWand: ((Float, Float) -> Unit)? = null,
    onLassoCompleted: ((List<StrokePoint>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentPoints = remember { mutableStateListOf<StrokePoint>() }
    var isDrawingActive by remember { mutableStateOf(false) }

    // Multi-touch tracking states
    var isTransforming by remember { mutableStateOf(false) }
    var waitingForFullRelease by remember { mutableStateOf(false) }
    var lastDistance by remember { mutableStateOf(0f) }
    var lastCenter by remember { mutableStateOf(Offset.Zero) }
    var lastAngle by remember { mutableStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("animation_canvas_container"),
        contentAlignment = Alignment.Center
    ) {
        val projWidth = project.resolution.width.toFloat()
        val projHeight = project.resolution.height.toFloat()
        val projAspect = projWidth / projHeight

        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val containerAspect = containerWidth / containerHeight

        // Base display size that fits container comfortably
        val baseWidth: Float
        val baseHeight: Float
        if (containerAspect > projAspect) {
            baseHeight = containerHeight * 0.85f
            baseWidth = baseHeight * projAspect
        } else {
            baseWidth = containerWidth * 0.90f
            baseHeight = baseWidth / projAspect
        }

        val density = LocalDensity.current

        // Helper to project screen touch to canvas resolution
        fun screenToProjectCoords(screenX: Float, screenY: Float): Offset {
            val canvasCenterX = containerWidth / 2f + panX
            val canvasCenterY = containerHeight / 2f + panY

            val dx = screenX - canvasCenterX
            val dy = screenY - canvasCenterY

            // Invert canvas rotation
            val rad = -Math.toRadians(rotation.toDouble())
            val cosRad = cos(rad).toFloat()
            val sinRad = sin(rad).toFloat()
            val unrotX = dx * cosRad - dy * sinRad
            val unrotY = dx * sinRad + dy * cosRad

            // Invert zoom scale
            val unscaledX = unrotX / zoom
            val unscaledY = unrotY / zoom

            // Re-offset to top-left of base canvas
            val localX = unscaledX + baseWidth / 2f
            val localY = unscaledY + baseHeight / 2f

            // Map base canvas coordinates to project resolution
            val px = localX * (projWidth / baseWidth)
            val py = localY * (projHeight / baseHeight)
            return Offset(px, py)
        }

        // Viewport touch interceptor for unified 1-finger draw & 2-finger zoom/pan/rotate
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { motionEvent ->
                    val pointerCount = motionEvent.pointerCount

                    // When 2 or more fingers are on screen: ZOOM, PAN & ROTATE
                    if (pointerCount >= 2) {
                        // Cancel any single-finger stroke that might have just started
                        if (isDrawingActive) {
                            currentPoints.clear()
                            isDrawingActive = false
                        }
                        waitingForFullRelease = true

                        val p0 = Offset(motionEvent.getX(0), motionEvent.getY(0))
                        val p1 = Offset(motionEvent.getX(1), motionEvent.getY(1))
                        val currentCenter = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                        val currentDist = hypot(p1.x - p0.x, p1.y - p0.y).coerceAtLeast(10f)
                        val currentAngle = Math.toDegrees(
                            atan2((p1.y - p0.y).toDouble(), (p1.x - p0.x).toDouble())
                        ).toFloat()

                        when (motionEvent.actionMasked) {
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                isTransforming = true
                                lastCenter = currentCenter
                                lastDistance = currentDist
                                lastAngle = currentAngle
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (isTransforming && lastDistance > 0f) {
                                    val zoomFactor = (currentDist / lastDistance).coerceIn(0.7f, 1.4f)
                                    val panDelta = currentCenter - lastCenter

                                    var angleDiff = currentAngle - lastAngle
                                    while (angleDiff > 180f) angleDiff -= 360f
                                    while (angleDiff < -180f) angleDiff += 360f

                                    onTransformChange(zoomFactor, panDelta.x, panDelta.y, angleDiff)

                                    lastCenter = currentCenter
                                    lastDistance = currentDist
                                    lastAngle = currentAngle
                                } else {
                                    isTransforming = true
                                    lastCenter = currentCenter
                                    lastDistance = currentDist
                                    lastAngle = currentAngle
                                }
                            }
                            MotionEvent.ACTION_POINTER_UP -> {
                                waitingForFullRelease = true
                            }
                        }
                        return@pointerInteropFilter true
                    }

                    // Single pointer handling
                    if (pointerCount == 1) {
                        // If we are waiting for user to completely lift their hand after a 2-finger gesture
                        if (waitingForFullRelease) {
                            if (motionEvent.actionMasked == MotionEvent.ACTION_UP ||
                                motionEvent.actionMasked == MotionEvent.ACTION_CANCEL
                            ) {
                                waitingForFullRelease = false
                                isTransforming = false
                                isDrawingActive = false
                                currentPoints.clear()
                            }
                            return@pointerInteropFilter true
                        }

                        // Stylus-only palm rejection check
                        val isStylus = motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
                        if (isStylusOnlyMode && !isStylus) {
                            return@pointerInteropFilter false
                        }

                        val pressure = motionEvent.pressure.coerceIn(0.2f, 2.0f)
                        val projPt = screenToProjectCoords(motionEvent.x, motionEvent.y)

                        when (motionEvent.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                if (selectedTool == ToolType.BUCKET) {
                                    onBucketFill?.invoke(projPt.x, projPt.y)
                                    isDrawingActive = false
                                    return@pointerInteropFilter true
                                }
                                if (selectedTool == ToolType.MAGIC_WAND) {
                                    onMagicWand?.invoke(projPt.x, projPt.y)
                                    isDrawingActive = false
                                    return@pointerInteropFilter true
                                }

                                isDrawingActive = true
                                currentPoints.clear()
                                currentPoints.add(StrokePoint(projPt.x, projPt.y, pressure))

                                if (selectedTool == ToolType.EYEDROPPER) {
                                    val currFrame = project.frames.getOrNull(currentFrameIndex)
                                    if (currFrame != null) {
                                        val bmp = KodanimaGraphicsEngine.renderFrameToBitmap(
                                            currFrame,
                                            project.resolution.width,
                                            project.resolution.height,
                                            android.graphics.Color.WHITE
                                        )
                                        val px = projPt.x.toInt().coerceIn(0, bmp.width - 1)
                                        val py = projPt.y.toInt().coerceIn(0, bmp.height - 1)
                                        val pickedColor = bmp.getPixel(px, py).toLong() and 0xFFFFFFFFL
                                        bmp.recycle()
                                        onEyedropperColorPicked(pickedColor)
                                    }
                                }
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (isDrawingActive) {
                                    val historySize = motionEvent.historySize
                                    for (h in 0 until historySize) {
                                        val histPt = screenToProjectCoords(
                                            motionEvent.getHistoricalX(h),
                                            motionEvent.getHistoricalY(h)
                                        )
                                        val hp = motionEvent.getHistoricalPressure(h).coerceIn(0.2f, 2.0f)
                                        currentPoints.add(StrokePoint(histPt.x, histPt.y, hp))
                                    }
                                    currentPoints.add(StrokePoint(projPt.x, projPt.y, pressure))
                                }
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                if (isDrawingActive) {
                                    currentPoints.add(StrokePoint(projPt.x, projPt.y, pressure))
                                    if (selectedTool == ToolType.LASSO) {
                                        onLassoCompleted?.invoke(currentPoints.toList())
                                    } else if (selectedTool != ToolType.EYEDROPPER &&
                                        selectedTool != ToolType.BUCKET &&
                                        selectedTool != ToolType.MAGIC_WAND &&
                                        currentPoints.isNotEmpty()
                                    ) {
                                        onStrokeCompleted(
                                            DrawingStroke(
                                                tool = selectedTool,
                                                color = currentColor,
                                                brushSize = brushSize,
                                                opacity = brushOpacity,
                                                points = currentPoints.toList()
                                            )
                                        )
                                    }
                                    currentPoints.clear()
                                    isDrawingActive = false
                                }
                                isTransforming = false
                                waitingForFullRelease = false
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // The Canvas Paper with transformation applied
            Box(
                modifier = Modifier
                    .offset { IntOffset(panX.roundToInt(), panY.roundToInt()) }
                    .graphicsLayer {
                        this.scaleX = zoom
                        this.scaleY = zoom
                        this.rotationZ = rotation
                    }
                    .size(
                        width = with(density) { baseWidth.toDp() },
                        height = with(density) { baseHeight.toDp() }
                    )
                    .shadow(16.dp, RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("drawing_canvas")
                ) {
                    val canvasScaleX = size.width / projWidth
                    val canvasScaleY = size.height / projHeight
                    val nativeCanvas = drawContext.canvas.nativeCanvas

                    // 1. Draw Onion Skin (Camada Cebola)
                    if (onionSkinEnabled && project.onionSkin.isEnabled) {
                        val prevCount = project.onionSkin.prevFramesCount
                        val nextCount = project.onionSkin.nextFramesCount
                        val onionOpacity = project.onionSkin.opacity

                        // Previous frames (tinted red / pink)
                        for (step in prevCount downTo 1) {
                            val prevIndex = currentFrameIndex - step
                            if (prevIndex >= 0 && prevIndex < project.frames.size) {
                                val alpha = (onionOpacity * (1f - (step - 1) * 0.25f)).coerceAtLeast(0.1f)
                                val filter = KodanimaGraphicsEngine.createOnionSkinColorFilter(project.onionSkin.prevColor)
                                nativeCanvas.save()
                                nativeCanvas.scale(canvasScaleX, canvasScaleY)
                                for (layer in project.frames[prevIndex].layers) {
                                    if (layer.isVisible) {
                                        KodanimaGraphicsEngine.drawLayerToCanvas(
                                            layer,
                                            nativeCanvas,
                                            project.resolution.width,
                                            project.resolution.height,
                                            alpha,
                                            filter
                                        )
                                    }
                                }
                                nativeCanvas.restore()
                            }
                        }

                        // Next frames (tinted cyan / blue)
                        for (step in 1..nextCount) {
                            val nextIndex = currentFrameIndex + step
                            if (nextIndex < project.frames.size) {
                                val alpha = (onionOpacity * (1f - (step - 1) * 0.25f)).coerceAtLeast(0.1f)
                                val filter = KodanimaGraphicsEngine.createOnionSkinColorFilter(project.onionSkin.nextColor)
                                nativeCanvas.save()
                                nativeCanvas.scale(canvasScaleX, canvasScaleY)
                                for (layer in project.frames[nextIndex].layers) {
                                    if (layer.isVisible) {
                                        KodanimaGraphicsEngine.drawLayerToCanvas(
                                            layer,
                                            nativeCanvas,
                                            project.resolution.width,
                                            project.resolution.height,
                                            alpha,
                                            filter
                                        )
                                    }
                                }
                                nativeCanvas.restore()
                            }
                        }
                    }

                    // 2. Draw Current Frame (full fidelity)
                    val currFrame = project.frames.getOrNull(currentFrameIndex)
                    if (currFrame != null) {
                        nativeCanvas.save()
                        nativeCanvas.scale(canvasScaleX, canvasScaleY)
                        for (layer in currFrame.layers) {
                            if (layer.isVisible) {
                                KodanimaGraphicsEngine.drawLayerToCanvas(
                                    layer,
                                    nativeCanvas,
                                    project.resolution.width,
                                    project.resolution.height,
                                    1.0f,
                                    null
                                )
                            }
                        }
                        nativeCanvas.restore()
                    }

                    // 3. Draw In-Progress Stroke Preview
                    if (currentPoints.isNotEmpty()) {
                        nativeCanvas.save()
                        nativeCanvas.scale(canvasScaleX, canvasScaleY)
                        val previewStroke = DrawingStroke(
                            tool = selectedTool,
                            color = currentColor,
                            brushSize = brushSize,
                            opacity = brushOpacity,
                            points = currentPoints.toList()
                        )
                        KodanimaGraphicsEngine.drawStrokeToCanvas(previewStroke, nativeCanvas, 1.0f, null)
                        nativeCanvas.restore()
                    }

                    // 4. Draw Active Selection Outline (Marching Ants / Wand / Lasso)
                    if (activeSelection.isActive && activeSelection.points.size >= 2) {
                        nativeCanvas.save()
                        nativeCanvas.scale(canvasScaleX, canvasScaleY)
                        val selPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.CYAN
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 2.0f / canvasScaleX
                            pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 6f), 0f)
                        }
                        val selPath = android.graphics.Path().apply {
                            val first = activeSelection.points.first()
                            moveTo(first.x, first.y)
                            for (p in activeSelection.points) {
                                lineTo(p.x, p.y)
                            }
                        }
                        nativeCanvas.drawPath(selPath, selPaint)
                        nativeCanvas.restore()
                    }
                }
            }
        }
    }
}
