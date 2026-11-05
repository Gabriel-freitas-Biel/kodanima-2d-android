package com.example.engine

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance animated GIF encoder implemented in pure Kotlin for Android.
 * Enables zero-dependency GIF exports for Kodanima 2D animations.
 */
class GifEncoder {
    private var width: Int = 0
    private var height: Int = 0
    private var delayMs: Int = 100 // 10 fps default
    private var repeatCount: Int = 0 // 0 = infinite loop
    private var isFirstFrame: Boolean = true

    fun setDelay(fps: Int) {
        val delayHundredths = max(1, 100 / max(1, fps))
        this.delayMs = delayHundredths * 10
    }

    fun setRepeat(count: Int) {
        this.repeatCount = count
    }

    /**
     * Encodes a list of bitmaps to animated GIF byte array.
     */
    fun encode(bitmaps: List<Bitmap>, fps: Int): ByteArray {
        val out = ByteArrayOutputStream()
        setDelay(fps)
        isFirstFrame = true

        if (bitmaps.isEmpty()) return ByteArray(0)

        for (bitmap in bitmaps) {
            addFrame(bitmap, out)
        }
        finish(out)
        return out.toByteArray()
    }

    private fun addFrame(bitmap: Bitmap, out: OutputStream) {
        val w = bitmap.width
        val h = bitmap.height

        if (isFirstFrame) {
            this.width = w
            this.height = h
            writeHeader(out)
            writeLogicalScreenDescriptor(out)
            writeNetscapeExtension(out)
        }

        // Downsample pixels to 256 colors palette
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val quantizer = ColorQuantizer(pixels)
        val indexedPixels = quantizer.indexedPixels
        val colorTable = quantizer.colorTable

        writeGraphicControlExtension(out, quantizer.transparentIndex)
        writeImageDescriptor(out, w, h, colorTable)
        writeLzwData(indexedPixels, quantizer.colorTableSize, out)

        isFirstFrame = false
    }

    private fun writeHeader(out: OutputStream) {
        // "GIF89a"
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
    }

    private fun writeLogicalScreenDescriptor(out: OutputStream) {
        writeShort(out, width)
        writeShort(out, height)
        // Global color table flag = 0 (we use local color tables per frame for better quality)
        out.write(0x70) // Color resolution = 8 bits, no global color table
        out.write(0)    // Background color index
        out.write(0)    // Pixel aspect ratio
    }

    private fun writeNetscapeExtension(out: OutputStream) {
        out.write(0x21) // Extension Introducer
        out.write(0xFF) // Application Extension Label
        out.write(11)   // Block Size
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(3)    // Sub-block size
        out.write(1)    // Loop sub-block ID
        writeShort(out, repeatCount)
        out.write(0)    // Block Terminator
    }

    private fun writeGraphicControlExtension(out: OutputStream, transparentIndex: Int) {
        out.write(0x21) // Extension Introducer
        out.write(0xF9) // Graphic Control Label
        out.write(4)    // Block Size

        val delayHundredths = max(1, delayMs / 10)
        var packed = 0x04 // Disposal Method = 1 (do not dispose / leave in place)
        if (transparentIndex >= 0) {
            packed = packed or 0x01 // Transparency flag
        }
        out.write(packed)
        writeShort(out, delayHundredths)
        out.write(if (transparentIndex >= 0) transparentIndex else 0)
        out.write(0)    // Block Terminator
    }

    private fun writeImageDescriptor(out: OutputStream, w: Int, h: Int, colorTable: ByteArray) {
        out.write(0x2C) // Image Separator
        writeShort(out, 0) // Left
        writeShort(out, 0) // Top
        writeShort(out, w)
        writeShort(out, h)

        // Local color table flag = 1, not interlaced, sorted = 0, size = 2^(7+1) = 256
        out.write(0x87)

        // Write local color table (768 bytes: 256 * RGB)
        out.write(colorTable)
    }

    private fun writeLzwData(pixels: ByteArray, colorCount: Int, out: OutputStream) {
        val initCodeSize = max(2, colorCountToBits(colorCount))
        out.write(initCodeSize)

        val lzw = LzwCompressor(pixels, initCodeSize)
        lzw.compress(out)

        out.write(0) // Block Terminator
    }

    private fun finish(out: OutputStream) {
        out.write(0x3B) // GIF Trailer
    }

    private fun writeShort(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    private fun colorCountToBits(count: Int): Int {
        var bits = 1
        while ((1 shl bits) < count) {
            bits++
        }
        return bits
    }

    /**
     * Fast quantizer for converting 32-bit ARGB pixels into an indexed 256-color palette.
     */
    private class ColorQuantizer(pixels: IntArray) {
        val colorTable = ByteArray(256 * 3)
        val indexedPixels = ByteArray(pixels.size)
        var transparentIndex: Int = -1
        val colorTableSize = 256

        init {
            // Build frequency map
            val colorMap = HashMap<Int, Int>(256)
            val uniqueColors = ArrayList<Int>(256)

            // Transparent color check
            var hasTransparency = false
            for (p in pixels) {
                val alpha = (p ushr 24) and 0xFF
                if (alpha < 128) {
                    hasTransparency = true
                    break
                }
            }

            if (hasTransparency) {
                transparentIndex = 0
                colorTable[0] = 0
                colorTable[1] = 0
                colorTable[2] = 0
            }

            // Simple sample to pick popular colors
            val step = max(1, pixels.size / 2000)
            for (i in pixels.indices step step) {
                val p = pixels[i]
                val alpha = (p ushr 24) and 0xFF
                if (alpha < 128) continue

                // Quantize to 5-bit per channel to cluster
                val r = (p ushr 16) and 0xF8
                val g = (p ushr 8) and 0xF8
                val b = p and 0xF8
                val rgb = (r shl 16) or (g shl 8) or b

                if (!colorMap.containsKey(rgb)) {
                    if (uniqueColors.size < (if (hasTransparency) 255 else 256)) {
                        colorMap[rgb] = uniqueColors.size + (if (hasTransparency) 1 else 0)
                        uniqueColors.add(rgb)
                    }
                }
            }

            // If not enough colors, fill with gray/white/black defaults
            if (uniqueColors.isEmpty()) {
                uniqueColors.add(0xFFFFFF)
                colorMap[0xFFFFFF] = if (hasTransparency) 1 else 0
            }

            // Fill color table
            for (i in uniqueColors.indices) {
                val c = uniqueColors[i]
                val index = (i + (if (hasTransparency) 1 else 0)) * 3
                colorTable[index] = ((c ushr 16) and 0xFF).toByte()
                colorTable[index + 1] = ((c ushr 8) and 0xFF).toByte()
                colorTable[index + 2] = (c and 0xFF).toByte()
            }

            // Map all pixels to nearest color index
            for (i in pixels.indices) {
                val p = pixels[i]
                val alpha = (p ushr 24) and 0xFF
                if (alpha < 128 && hasTransparency) {
                    indexedPixels[i] = transparentIndex.toByte()
                    continue
                }

                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                val rgb = ((r and 0xF8) shl 16) or ((g and 0xF8) shl 8) or (b and 0xF8)

                val mapped = colorMap[rgb]
                if (mapped != null) {
                    indexedPixels[i] = mapped.toByte()
                } else {
                    // Find closest color
                    var closestDist = Int.MAX_VALUE
                    var closestIdx = if (hasTransparency) 1 else 0
                    val start = if (hasTransparency) 1 else 0
                    for (cIdx in start until (uniqueColors.size + start)) {
                        val cR = colorTable[cIdx * 3].toInt() and 0xFF
                        val cG = colorTable[cIdx * 3 + 1].toInt() and 0xFF
                        val cB = colorTable[cIdx * 3 + 2].toInt() and 0xFF
                        val dr = r - cR
                        val dg = g - cG
                        val db = b - cB
                        val dist = dr * dr + dg * dg + db * db
                        if (dist < closestDist) {
                            closestDist = dist
                            closestIdx = cIdx
                        }
                    }
                    indexedPixels[i] = closestIdx.toByte()
                }
            }
        }
    }

    /**
     * Standard LZW compressor for GIF data streams.
     */
    private class LzwCompressor(
        private val pixels: ByteArray,
        private val initCodeSize: Int
    ) {
        private val clearCode = 1 shl initCodeSize
        private val eofCode = clearCode + 1
        private var codeSize = initCodeSize + 1
        private var maxCode = (1 shl codeSize) - 1

        private val buffer = ByteArray(256)
        private var bufIndex = 0
        private var curAccum = 0
        private var curBits = 0

        fun compress(out: OutputStream) {
            val hTab = IntArray(5003) { -1 }
            val codeTab = IntArray(5003)

            output(clearCode, out)
            var ent = if (pixels.isNotEmpty()) pixels[0].toInt() and 0xFF else -1
            var freeEnt = clearCode + 2

            for (i in 1 until pixels.size) {
                val c = pixels[i].toInt() and 0xFF
                val fcode = (c shl 12) + ent
                var idx = (c shl 4) xor ent
                if (idx >= 5003) idx %= 5003

                if (hTab[idx] == fcode) {
                    ent = codeTab[idx]
                    continue
                } else if (hTab[idx] >= 0) {
                    var disp = 5003 - idx
                    if (idx == 0) disp = 1
                    var found = false
                    while (true) {
                        idx -= disp
                        if (idx < 0) idx += 5003
                        if (hTab[idx] == fcode) {
                            ent = codeTab[idx]
                            found = true
                            break
                        }
                        if (hTab[idx] < 0) break
                    }
                    if (found) continue
                }

                output(ent, out)
                ent = c
                if (freeEnt < 4096) {
                    codeTab[idx] = freeEnt++
                    hTab[idx] = fcode
                } else {
                    hTab.fill(-1)
                    freeEnt = clearCode + 2
                    output(clearCode, out)
                    codeSize = initCodeSize + 1
                    maxCode = (1 shl codeSize) - 1
                }
            }

            if (ent != -1) {
                output(ent, out)
            }
            output(eofCode, out)
            flushBits(out)
            flushPacket(out)
        }

        private fun output(code: Int, out: OutputStream) {
            curAccum = curAccum or (code shl curBits)
            curBits += codeSize

            while (curBits >= 8) {
                charOut((curAccum and 0xFF).toByte(), out)
                curAccum = curAccum ushr 8
                curBits -= 8
            }

            if (freeEntCheck(code)) {
                codeSize++
                maxCode = (1 shl codeSize) - 1
            }
        }

        private fun freeEntCheck(code: Int): Boolean {
            return code > maxCode && codeSize < 12
        }

        private fun flushBits(out: OutputStream) {
            if (curBits > 0) {
                charOut((curAccum and 0xFF).toByte(), out)
                curAccum = 0
                curBits = 0
            }
        }

        private fun charOut(c: Byte, out: OutputStream) {
            buffer[bufIndex++] = c
            if (bufIndex >= 254) {
                flushPacket(out)
            }
        }

        private fun flushPacket(out: OutputStream) {
            if (bufIndex > 0) {
                out.write(bufIndex)
                out.write(buffer, 0, bufIndex)
                bufIndex = 0
            }
        }
    }
}
