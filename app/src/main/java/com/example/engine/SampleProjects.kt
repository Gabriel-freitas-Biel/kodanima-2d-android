package com.example.engine

import com.example.model.AnimationProject
import com.example.model.DrawingStroke
import com.example.model.Frame
import com.example.model.Layer
import com.example.model.OnionSkinSettings
import com.example.model.ProjectResolution
import com.example.model.StrokePoint
import com.example.model.ToolType
import java.util.UUID

object SampleProjects {

    /**
     * Creates the classic "Bouncing Ball" animation demonstration showcasing
     * squash and stretch, arc of motion, onion skinning and timing.
     */
    fun createBouncingBallProject(): AnimationProject {
        val width = 1280
        val height = 720
        val cx = width / 2f
        val groundY = 560f

        // Ball trajectory coordinates and squash factors across 8 frames
        // (yCenter, radiusX, radiusY, isSquashed)
        val frameData = listOf(
            Triple(200f, 60f, 60f), // 1. Peak top
            Triple(280f, 58f, 62f), // 2. Falling
            Triple(420f, 52f, 75f), // 3. Accelerating downwards (stretch)
            Triple(530f, 85f, 35f), // 4. Impact ground (squash)
            Triple(420f, 54f, 72f), // 5. Rebound launch (stretch)
            Triple(290f, 58f, 62f), // 6. Ascending
            Triple(220f, 60f, 60f), // 7. Slowing near peak
            Triple(200f, 60f, 60f)  // 8. Apex
        )

        val frames = frameData.mapIndexed { idx, (ballY, rx, ry) ->
            // Ground layer (constant across frames)
            val groundStroke = DrawingStroke(
                tool = ToolType.LINE,
                color = 0xFF555A6E,
                brushSize = 4f,
                points = listOf(
                    StrokePoint(160f, groundY),
                    StrokePoint(1120f, groundY)
                )
            )

            // Shadow on ground
            val shadowWidth = rx * 1.2f
            val shadowPoints = (0..16).map { i ->
                val angle = i * (Math.PI / 8)
                val sx = cx + (shadowWidth * kotlin.math.cos(angle)).toFloat()
                val sy = groundY + 12f + (10f * kotlin.math.sin(angle)).toFloat()
                StrokePoint(sx, sy)
            }
            val shadowStroke = DrawingStroke(
                tool = ToolType.CIRCLE,
                color = 0x66202433,
                brushSize = 3f,
                points = shadowPoints,
                isFilled = true
            )

            // Animated ball with vibrant Kodanima cyan outline and neon glow
            val ballPoints = (0..32).map { i ->
                val angle = i * (2 * Math.PI / 32)
                val bx = cx + (rx * kotlin.math.cos(angle)).toFloat()
                val by = ballY + (ry * kotlin.math.sin(angle)).toFloat()
                StrokePoint(bx, by, pressure = 1.0f)
            }
            val ballStroke = DrawingStroke(
                tool = ToolType.CIRCLE,
                color = 0xFF00D2FF, // Kodanima Cyan
                brushSize = 8f,
                points = ballPoints,
                isFilled = true
            )

            // Ball highlight detail
            val highlightPoints = listOf(
                StrokePoint(cx - rx * 0.35f, ballY - ry * 0.35f),
                StrokePoint(cx - rx * 0.15f, ballY - ry * 0.25f)
            )
            val highlightStroke = DrawingStroke(
                tool = ToolType.PEN,
                color = 0xFFFFFFFF,
                brushSize = 5f,
                points = highlightPoints
            )

            Frame(
                id = UUID.randomUUID().toString(),
                layers = listOf(
                    Layer(
                        id = UUID.randomUUID().toString(),
                        name = "Cenário / Chão",
                        strokes = listOf(groundStroke, shadowStroke)
                    ),
                    Layer(
                        id = UUID.randomUUID().toString(),
                        name = "Animação Bola",
                        strokes = listOf(ballStroke, highlightStroke)
                    )
                ),
                holdFrames = 1
            )
        }

        val sampleAudio = com.example.model.SpecialMediaAsset(
            id = "sample_audio_1",
            name = "Música 1 (Trilha Sonora)",
            type = "audio",
            startSeconds = 0.0,
            durationSeconds = (frames.size.toDouble() / 12.0),
            audioVolumePercent = 90,
            waveformPeaks = listOf(0.2f, 0.5f, 0.9f, 0.6f, 0.8f, 0.4f, 0.95f, 0.3f, 0.7f, 0.5f, 0.85f, 0.4f)
        )
        val sampleVideo = com.example.model.SpecialMediaAsset(
            id = "sample_video_1",
            name = "Vídeo / GIF (Referência de Movimento)",
            type = "video",
            startSeconds = 0.0,
            durationSeconds = (frames.size.toDouble() / 12.0),
            opacityPercent = 50,
            scalePercent = 100.0
        )

        return AnimationProject(
            id = "demo_bouncing_ball",
            name = "Demo: Bouncing Ball (Squash & Stretch)",
            resolution = ProjectResolution.PRESETS[0],
            fps = 12,
            frames = frames,
            mediaAssets = listOf(sampleAudio, sampleVideo),
            onionSkin = OnionSkinSettings(
                isEnabled = true,
                prevFramesCount = 2,
                nextFramesCount = 1,
                opacity = 0.4f
            )
        )
    }

    /**
     * Creates a blank new project with standard settings.
     */
    fun createEmptyProject(
        name: String = "Novo Projeto 2D",
        resolution: ProjectResolution = ProjectResolution.PRESETS[0],
        fps: Int = 12
    ): AnimationProject {
        return AnimationProject(
            name = name,
            resolution = resolution,
            fps = fps,
            frames = listOf(Frame(layers = listOf(Layer(name = "Camada 1")))),
            onionSkin = OnionSkinSettings(isEnabled = true)
        )
    }
}
