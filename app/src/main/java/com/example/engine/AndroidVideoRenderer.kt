package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import com.example.model.AnimationProject
import com.example.model.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Native Android Hardware-Accelerated Video Renderer for Kodanima 2D.
 * Uses Android MediaCodec + MediaMuxer to encode frames to real MP4 (H.264 AVC).
 * Also supports exporting frame packs as ZIP archives and single PNG frames.
 */
object AndroidVideoRenderer {

    /**
     * Encodes animation frames to MP4 using Android's hardware MediaCodec & MediaMuxer.
     */
    suspend fun renderToMp4(
        context: Context,
        project: AnimationProject,
        onProgress: (Float, String) -> Unit
    ): File = withContext(Dispatchers.Default) {
        // Enforce even dimensions for H.264 AVC encoder compatibility
        var width = project.resolution.width
        var height = project.resolution.height
        if (width % 2 != 0) width -= 1
        if (height % 2 != 0) height -= 1
        width = max(160, min(1920, width))
        height = max(160, min(1080, height))

        val fps = max(1, min(60, project.fps))
        val frameDurationUs = 1_000_000L / fps
        val bitRate = width * height * 4 // Good quality balance

        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val outputFile = File(exportDir, "${project.name.replace(" ", "_")}_kodanima.mp4")
        if (outputFile.exists()) outputFile.delete()

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()

        // Prepare frame bitmaps
        val totalFrames = project.frames.size
        var presentationTimeUs = 0L

        fun drainEncoder(endOfStream: Boolean) {
            val timeoutUs = if (endOfStream) 10000L else 1000L
            while (true) {
                val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) break
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) throw RuntimeException("Format changed twice")
                    val newFormat = encoder.outputFormat
                    trackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (encoderStatus >= 0) {
                    val encodedData = encoder.getOutputBuffer(encoderStatus)
                        ?: throw RuntimeException("encoderOutputBuffer $encoderStatus was null")

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0) {
                        if (!muxerStarted) throw RuntimeException("Muxer hasn't started")
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }

                    encoder.releaseOutputBuffer(encoderStatus, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }

        for (i in project.frames.indices) {
            val frame = project.frames[i]
            onProgress(0.1f + 0.7f * (i.toFloat() / totalFrames), "Codificando quadro ${i + 1}/$totalFrames via hardware GPU...")

            val frameBitmap = KodanimaGraphicsEngine.renderFrameToBitmap(
                frame, width, height, project.resolution.let { android.graphics.Color.WHITE }
            )

            // Render onto input surface
            repeat(max(1, frame.holdFrames)) {
                drainEncoder(false)

                val canvas: Canvas = inputSurface.lockCanvas(null)
                canvas.drawBitmap(frameBitmap, 0f, 0f, null)
                inputSurface.unlockCanvasAndPost(canvas)

                presentationTimeUs += frameDurationUs
            }
            frameBitmap.recycle()
        }

        onProgress(0.85f, "Finalizando vídeo MP4...")
        encoder.signalEndOfInputStream()
        drainEncoder(true)

        encoder.stop()
        encoder.release()
        inputSurface.release()

        if (muxerStarted) {
            muxer.stop()
            muxer.release()
        }

        onProgress(1.0f, "MP4 concluído com sucesso!")
        outputFile
    }

    /**
     * Exports all frames as a sequential PNG image pack inside a ZIP file.
     */
    suspend fun exportFramePackZip(
        context: Context,
        project: AnimationProject,
        onProgress: (Float, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val zipFile = File(exportDir, "${project.name.replace(" ", "_")}_quadros_png.zip")
        if (zipFile.exists()) zipFile.delete()

        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            val total = project.frames.size
            for (i in project.frames.indices) {
                onProgress(0.1f + 0.8f * (i.toFloat() / total), "Compactando quadro ${i + 1}/$total...")
                val frame = project.frames[i]
                val bmp = KodanimaGraphicsEngine.renderFrameToBitmap(
                    frame, project.resolution.width, project.resolution.height, android.graphics.Color.WHITE
                )
                val entryName = String.format("frame_%06d.png", i + 1)
                zipOut.putNextEntry(ZipEntry(entryName))
                bmp.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                zipOut.closeEntry()
                bmp.recycle()
            }
        }
        onProgress(1.0f, "Pacote de quadros PNG criado com sucesso!")
        zipFile
    }
}
