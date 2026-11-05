package com.example.model

import java.util.UUID

enum class ToolType(val labelPt: String, val labelEn: String) {
    PEN("Caneta", "Pen"),
    PENCIL("Lápis", "Pencil"),
    BRUSH("Pincel", "Brush"),
    ERASER("Borracha", "Eraser"),
    BUCKET("Balde", "Bucket"),
    MAGIC_WAND("Varinha Mágica", "Magic Wand"),
    LASSO("Laço", "Lasso"),
    LINE("Linha", "Line"),
    RECTANGLE("Retângulo", "Rectangle"),
    CIRCLE("Círculo", "Circle"),
    EYEDROPPER("Conta-gotas", "Eyedropper")
}

data class SelectionRegion(
    val isActive: Boolean = false,
    val points: List<StrokePoint> = emptyList()
)

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class DrawingStroke(
    val id: String = UUID.randomUUID().toString(),
    val tool: ToolType = ToolType.PEN,
    val color: Long = 0xFFFFFFFF, // ARGB Long
    val brushSize: Float = 6f,
    val opacity: Float = 1.0f,
    val points: List<StrokePoint> = emptyList(),
    val isFilled: Boolean = false
)

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Camada 1",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f,
    val strokes: List<DrawingStroke> = emptyList(),
    val referenceImageUri: String? = null
)

data class Frame(
    val id: String = UUID.randomUUID().toString(),
    val layers: List<Layer> = listOf(Layer()),
    val holdFrames: Int = 1 // Exposure hold (1 = 1 frame, 2 = held on 2s)
)

data class SpecialMediaAsset(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Mídia",
    val uriString: String = "",
    val type: String = "audio", // "audio", "video", "gif"
    val startSeconds: Double = 0.0,
    val durationSeconds: Double = 0.0,
    val opacityPercent: Int = 100,
    val scalePercent: Double = 100.0,
    val rotationDegrees: Double = 0.0,
    val audioVolumePercent: Int = 100,
    val audioMuted: Boolean = false,
    val waveformPeaks: List<Float> = emptyList()
)

data class ProjectResolution(
    val width: Int = 1280,
    val height: Int = 720,
    val name: String = "HD 720p (16:9)"
) {
    companion object {
        val PRESETS = listOf(
            ProjectResolution(1280, 720, "HD 720p (16:9)"),
            ProjectResolution(1920, 1080, "Full HD 1080p (16:9)"),
            ProjectResolution(1080, 1080, "Instagram Post (1:1)"),
            ProjectResolution(1080, 1920, "Stories / TikTok (9:16)"),
            ProjectResolution(1024, 768, "Clássico 4:3"),
            ProjectResolution(640, 360, "Rápido / Leve (16:9)"),
            ProjectResolution(320, 320, "Pixel Art (1:1)")
        )
    }
}

data class OnionSkinSettings(
    val isEnabled: Boolean = true,
    val prevFramesCount: Int = 2,
    val nextFramesCount: Int = 1,
    val opacity: Float = 0.35f,
    val prevColor: Long = 0xFFFF3366, // Neon red/pink for past frames
    val nextColor: Long = 0xFF00D2FF  // Neon cyan for future frames
)

data class ReferenceImageState(
    val uriString: String? = null,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1.0f,
    val opacity: Float = 0.5f,
    val isVisible: Boolean = true
)

data class AnimationProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Nova Animação",
    val resolution: ProjectResolution = ProjectResolution.PRESETS[0],
    val fps: Int = 12,
    val frames: List<Frame> = listOf(Frame()),
    val mediaAssets: List<SpecialMediaAsset> = emptyList(),
    val onionSkin: OnionSkinSettings = OnionSkinSettings(),
    val lastActiveFrameIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
