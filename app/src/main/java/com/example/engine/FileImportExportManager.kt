package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.AnimationProject
import com.example.model.DrawingStroke
import com.example.model.Frame
import com.example.model.Layer
import com.example.model.OnionSkinSettings
import com.example.model.ProjectResolution
import com.example.model.StrokePoint
import com.example.model.ToolType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * File Import & Export Manager for Kodanima 2D on Android.
 * Handles:
 * - Image decoding from Android content URIs
 * - Project serialization/deserialization (.kodanima format)
 * - GIF / PNG export and sharing via Android Sharesheet
 */
object FileImportExportManager {

    /**
     * Decodes an image from content Uri safely.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1920): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeStream = context.contentResolver.openInputStream(uri)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val result = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
            decodeStream?.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Serializes an AnimationProject to JSON string (.kodanima format).
     */
    fun projectToJson(project: AnimationProject): String {
        val root = JSONObject()
        root.put("version", "9.2.0-android")
        root.put("id", project.id)
        root.put("name", project.name)
        root.put("fps", project.fps)
        root.put("createdAt", project.createdAt)
        root.put("updatedAt", project.updatedAt)

        // Resolution
        val res = JSONObject()
        res.put("width", project.resolution.width)
        res.put("height", project.resolution.height)
        res.put("name", project.resolution.name)
        root.put("resolution", res)

        // Onion Skin
        val os = JSONObject()
        os.put("isEnabled", project.onionSkin.isEnabled)
        os.put("prevFramesCount", project.onionSkin.prevFramesCount)
        os.put("nextFramesCount", project.onionSkin.nextFramesCount)
        os.put("opacity", project.onionSkin.opacity.toDouble())
        os.put("prevColor", project.onionSkin.prevColor)
        os.put("nextColor", project.onionSkin.nextColor)
        root.put("onionSkin", os)

        // Frames
        val framesArray = JSONArray()
        for (frame in project.frames) {
            val frameObj = JSONObject()
            frameObj.put("id", frame.id)
            frameObj.put("holdFrames", frame.holdFrames)

            val layersArray = JSONArray()
            for (layer in frame.layers) {
                val layerObj = JSONObject()
                layerObj.put("id", layer.id)
                layerObj.put("name", layer.name)
                layerObj.put("isVisible", layer.isVisible)
                layerObj.put("isLocked", layer.isLocked)
                layerObj.put("opacity", layer.opacity.toDouble())

                val strokesArray = JSONArray()
                for (stroke in layer.strokes) {
                    val strokeObj = JSONObject()
                    strokeObj.put("id", stroke.id)
                    strokeObj.put("tool", stroke.tool.name)
                    strokeObj.put("color", stroke.color)
                    strokeObj.put("brushSize", stroke.brushSize.toDouble())
                    strokeObj.put("opacity", stroke.opacity.toDouble())
                    strokeObj.put("isFilled", stroke.isFilled)

                    val pointsArray = JSONArray()
                    for (p in stroke.points) {
                        val pObj = JSONObject()
                        pObj.put("x", p.x.toDouble())
                        pObj.put("y", p.y.toDouble())
                        pObj.put("pressure", p.pressure.toDouble())
                        pointsArray.put(pObj)
                    }
                    strokeObj.put("points", pointsArray)
                    strokesArray.put(strokeObj)
                }
                layerObj.put("strokes", strokesArray)
                layersArray.put(layerObj)
            }
            frameObj.put("layers", layersArray)
            framesArray.put(frameObj)
        }
        // Media Assets (Audio & Video / GIF tracks matching desktop Kodanima2D)
        val mediaArray = JSONArray()
        for (asset in project.mediaAssets) {
            val aObj = JSONObject()
            aObj.put("id", asset.id)
            aObj.put("name", asset.name)
            aObj.put("uri", asset.uriString)
            aObj.put("type", asset.type)
            aObj.put("startSeconds", asset.startSeconds)
            aObj.put("durationSeconds", asset.durationSeconds)
            aObj.put("opacityPercent", asset.opacityPercent)
            aObj.put("scalePercent", asset.scalePercent)
            aObj.put("rotationDegrees", asset.rotationDegrees)
            aObj.put("audioVolumePercent", asset.audioVolumePercent)
            aObj.put("audioMuted", asset.audioMuted)
            val peaksArray = JSONArray()
            for (p in asset.waveformPeaks) peaksArray.put(p.toDouble())
            aObj.put("waveformPeaks", peaksArray)
            mediaArray.put(aObj)
        }
        root.put("mediaAssets", mediaArray)

        root.put("frames", framesArray)

        return root.toString(2)
    }

    /**
     * Parses an AnimationProject from JSON string (.kodanima format).
     */
    fun projectFromJson(jsonString: String): AnimationProject {
        val root = JSONObject(jsonString)
        val id = root.optString("id", java.util.UUID.randomUUID().toString())
        val name = root.optString("name", "Projeto Importado")
        val fps = root.optInt("fps", 12)
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())
        val updatedAt = root.optLong("updatedAt", System.currentTimeMillis())

        val resObj = root.optJSONObject("resolution")
        val resolution = if (resObj != null) {
            ProjectResolution(
                width = resObj.optInt("width", 1280),
                height = resObj.optInt("height", 720),
                name = resObj.optString("name", "Personalizado")
            )
        } else {
            ProjectResolution.PRESETS[0]
        }

        val osObj = root.optJSONObject("onionSkin")
        val onionSkin = if (osObj != null) {
            OnionSkinSettings(
                isEnabled = osObj.optBoolean("isEnabled", true),
                prevFramesCount = osObj.optInt("prevFramesCount", 2),
                nextFramesCount = osObj.optInt("nextFramesCount", 1),
                opacity = osObj.optDouble("opacity", 0.35).toFloat(),
                prevColor = osObj.optLong("prevColor", 0xFFFF3366),
                nextColor = osObj.optLong("nextColor", 0xFF00D2FF)
            )
        } else {
            OnionSkinSettings()
        }

        val framesList = ArrayList<Frame>()
        val framesArray = root.optJSONArray("frames")
        if (framesArray != null) {
            for (fIdx in 0 until framesArray.length()) {
                val fObj = framesArray.getJSONObject(fIdx)
                val fId = fObj.optString("id", java.util.UUID.randomUUID().toString())
                val hold = fObj.optInt("holdFrames", 1)

                val layersList = ArrayList<Layer>()
                val layersArray = fObj.optJSONArray("layers")
                if (layersArray != null) {
                    for (lIdx in 0 until layersArray.length()) {
                        val lObj = layersArray.getJSONObject(lIdx)
                        val lId = lObj.optString("id", java.util.UUID.randomUUID().toString())
                        val lName = lObj.optString("name", "Camada ${lIdx + 1}")
                        val isVisible = lObj.optBoolean("isVisible", true)
                        val isLocked = lObj.optBoolean("isLocked", false)
                        val opacity = lObj.optDouble("opacity", 1.0).toFloat()

                        val strokesList = ArrayList<DrawingStroke>()
                        val strokesArray = lObj.optJSONArray("strokes")
                        if (strokesArray != null) {
                            for (sIdx in 0 until strokesArray.length()) {
                                val sObj = strokesArray.getJSONObject(sIdx)
                                val sId = sObj.optString("id", java.util.UUID.randomUUID().toString())
                                val toolStr = sObj.optString("tool", "PEN")
                                val tool = try { ToolType.valueOf(toolStr) } catch (_: Exception) { ToolType.PEN }
                                val color = sObj.optLong("color", 0xFFFFFFFF)
                                val brushSize = sObj.optDouble("brushSize", 6.0).toFloat()
                                val strokeOpacity = sObj.optDouble("opacity", 1.0).toFloat()
                                val isFilled = sObj.optBoolean("isFilled", false)

                                val pointsList = ArrayList<StrokePoint>()
                                val pointsArray = sObj.optJSONArray("points")
                                if (pointsArray != null) {
                                    for (pIdx in 0 until pointsArray.length()) {
                                        val pObj = pointsArray.getJSONObject(pIdx)
                                        pointsList.add(
                                            StrokePoint(
                                                x = pObj.optDouble("x", 0.0).toFloat(),
                                                y = pObj.optDouble("y", 0.0).toFloat(),
                                                pressure = pObj.optDouble("pressure", 1.0).toFloat()
                                            )
                                        )
                                    }
                                }
                                strokesList.add(
                                    DrawingStroke(
                                        id = sId,
                                        tool = tool,
                                        color = color,
                                        brushSize = brushSize,
                                        opacity = strokeOpacity,
                                        points = pointsList,
                                        isFilled = isFilled
                                    )
                                )
                            }
                        }
                        layersList.add(
                            Layer(
                                id = lId,
                                name = lName,
                                isVisible = isVisible,
                                isLocked = isLocked,
                                opacity = opacity,
                                strokes = strokesList
                            )
                        )
                    }
                }
                framesList.add(Frame(id = fId, layers = if (layersList.isEmpty()) listOf(Layer()) else layersList, holdFrames = hold))
            }
        }

        val mediaAssetsList = ArrayList<com.example.model.SpecialMediaAsset>()
        val mediaArray = root.optJSONArray("mediaAssets")
        if (mediaArray != null) {
            for (mIdx in 0 until mediaArray.length()) {
                val mObj = mediaArray.getJSONObject(mIdx)
                val peaksList = ArrayList<Float>()
                val peaksArr = mObj.optJSONArray("waveformPeaks")
                if (peaksArr != null) {
                    for (p in 0 until peaksArr.length()) peaksList.add(peaksArr.optDouble(p, 0.0).toFloat())
                }
                mediaAssetsList.add(
                    com.example.model.SpecialMediaAsset(
                        id = mObj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = mObj.optString("name", "Mídia"),
                        uriString = mObj.optString("uri", ""),
                        type = mObj.optString("type", "audio"),
                        startSeconds = mObj.optDouble("startSeconds", 0.0),
                        durationSeconds = mObj.optDouble("durationSeconds", 0.0),
                        opacityPercent = mObj.optInt("opacityPercent", 100),
                        scalePercent = mObj.optDouble("scalePercent", 100.0),
                        rotationDegrees = mObj.optDouble("rotationDegrees", 0.0),
                        audioVolumePercent = mObj.optInt("audioVolumePercent", 100),
                        audioMuted = mObj.optBoolean("audioMuted", false),
                        waveformPeaks = peaksList
                    )
                )
            }
        }

        return AnimationProject(
            id = id,
            name = name,
            resolution = resolution,
            fps = fps,
            frames = if (framesList.isEmpty()) listOf(Frame()) else framesList,
            mediaAssets = mediaAssetsList,
            onionSkin = onionSkin,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * Saves bytes to application cache and returns share intent.
     */
    fun shareFile(context: Context, filename: String, mimeType: String, data: ByteArray) {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, filename)
            FileOutputStream(file).use { it.write(data) }

            val uri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (_: Exception) {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Animação Kodanima 2D").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
