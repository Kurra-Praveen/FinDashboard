package com.kpr.fintrack.utils.image

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.createBitmap
import com.kpr.fintrack.utils.FinTrackLogger

object ImageUtils {

    /**
     * Sorts text blocks by their visual position (top-to-bottom, left-to-right)
     * This ensures text maintains its original layout sequence
     */
     fun sortTextBlocksByPosition(visionText: com.google.mlkit.vision.text.Text): String {
        if (visionText.textBlocks.isEmpty()) {
            return ""
        }

        // Sort blocks by Y position (top to bottom) with grouping tolerance
        val sortedBlocks = visionText.textBlocks.sortedWith(compareBy<com.google.mlkit.vision.text.Text.TextBlock> { block ->
            block.boundingBox?.top ?: 0
        }.thenBy { block ->
            // Within same Y range, sort by X position (left to right)
            block.boundingBox?.left ?: 0
        })

        // Group blocks that are on roughly the same line
        val lineGroups = groupBlocksByLine(sortedBlocks)

        // Build the final text maintaining layout
        val result = StringBuilder()

        lineGroups.forEach { lineBlocks ->
            // Sort blocks within the same line by X position
            val sortedLine = lineBlocks.sortedBy { it.boundingBox?.left ?: 0 }

            sortedLine.forEachIndexed { index, block ->
                result.append(block.text)

                // Add space between blocks on same line if there's significant gap
                if (index < sortedLine.size - 1) {
                    val currentRight = block.boundingBox?.right ?: 0
                    val nextLeft = sortedLine[index + 1].boundingBox?.left ?: 0
                    val gap = nextLeft - currentRight

                    // If gap is larger than average character width, add space
                    val avgCharWidth = (block.boundingBox?.width() ?: 0) / maxOf(block.text.length, 1)
                    if (gap > avgCharWidth / 2) {
                        result.append(" ")
                    }
                }
            }

            // Add newline after each line group
            result.append("\n")
        }

        return result.toString().trim()
    }

    /**
     * Groups text blocks that appear on the same horizontal line
     * Uses vertical overlap/proximity to determine if blocks are on same line
     */
    private fun groupBlocksByLine(
        blocks: List<com.google.mlkit.vision.text.Text.TextBlock>
    ): List<List<com.google.mlkit.vision.text.Text.TextBlock>> {
        if (blocks.isEmpty()) return emptyList()

        val lines = mutableListOf<MutableList<com.google.mlkit.vision.text.Text.TextBlock>>()
        var currentLine = mutableListOf<com.google.mlkit.vision.text.Text.TextBlock>()

        blocks.forEachIndexed { index, block ->
            if (currentLine.isEmpty()) {
                // Start new line
                currentLine.add(block)
            } else {
                val lastBlock = currentLine.last()
                val lastBlockBox = lastBlock.boundingBox
                val currentBlockBox = block.boundingBox

                if (lastBlockBox != null && currentBlockBox != null) {
                    // Calculate vertical overlap/proximity
                    val lastBlockCenterY = lastBlockBox.top + lastBlockBox.height() / 2
                    val currentBlockCenterY = currentBlockBox.top + currentBlockBox.height() / 2
                    val avgHeight = (lastBlockBox.height() + currentBlockBox.height()) / 2
                    val verticalDistance = kotlin.math.abs(lastBlockCenterY - currentBlockCenterY)

                    // If blocks are roughly on same line (within 40% of average height)
                    if (verticalDistance < avgHeight * 0.4) {
                        currentLine.add(block)
                    } else {
                        // Start new line
                        lines.add(currentLine)
                        currentLine = mutableListOf(block)
                    }
                } else {
                    currentLine.add(block)
                }
            }
        }

        // Add last line
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    /**
     * Alternative: Sort by lines (simpler approach)
     * Use this if the above method is too complex for your use case
     */
    private fun sortTextBlocksByPositionSimple(visionText: com.google.mlkit.vision.text.Text): String {
        return visionText.textBlocks
            .sortedWith(compareBy<com.google.mlkit.vision.text.Text.TextBlock> {
                it.boundingBox?.top ?: 0
            }.thenBy {
                it.boundingBox?.left ?: 0
            })
            .joinToString("\n") { it.text }
    }
    /**
     * Preprocesses the image for better OCR accuracy using native Android APIs
     * No external dependencies required
     * Enhanced preprocessing for better OCR accuracy, especially for small text
     * Increases text intensity and clarity significantly
     */
     fun preprocessImageForOcrNative(bitmap: Bitmap): Bitmap {
        try {
            val width = bitmap.width
            val height = bitmap.height

            // Create output bitmap with proper config
            val enhancedBitmap = createBitmap(width, height)
            val canvas = Canvas(enhancedBitmap)

            val paint = Paint().apply {
                // Convert to grayscale using ColorMatrix
                val grayscaleMatrix = ColorMatrix().apply {
                    setSaturation(0f) // Grayscale
                }

                // INCREASED CONTRAST - More aggressive for small text
                val contrast = 2.0f  // Increased from 1.5f to 2.0f
                val scale = contrast + 1f
                val translate = (-.5f * scale + .5f) * 255f

                val contrastMatrix = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))

                // BRIGHTNESS ADJUSTMENT - Slightly brighten to help with dark backgrounds
                val brightnessAdjust = 10f  // Add slight brightness
                val brightnessMatrix = ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, brightnessAdjust,
                    0f, 1f, 0f, 0f, brightnessAdjust,
                    0f, 0f, 1f, 0f, brightnessAdjust,
                    0f, 0f, 0f, 1f, 0f
                ))

                grayscaleMatrix.postConcat(contrastMatrix)
                grayscaleMatrix.postConcat(brightnessMatrix)
                colorFilter = ColorMatrixColorFilter(grayscaleMatrix)
            }

            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            // Apply stronger sharpening for small text
            val sharpenedBitmap = sharpenBitmapEnhanced(enhancedBitmap)

            // Only recycle if we created a new bitmap
            if (sharpenedBitmap != enhancedBitmap) {
                enhancedBitmap.recycle()
            }

            FinTrackLogger.d("FinTrack_Image", "Enhanced image preprocessing completed successfully")
            return sharpenedBitmap
        } catch (e: Exception) {
            FinTrackLogger.d("FinTrack_Image", "Image preprocessing failed, using original: ${e.message}")
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * Enhanced sharpening filter - stronger for small text clarity
     * Uses a more aggressive kernel for better edge detection
     */
    private fun sharpenBitmapEnhanced(bitmap: Bitmap): Bitmap {
        try {
            val width = bitmap.width
            val height = bitmap.height

            // Create output bitmap
            val sharpenedBitmap = createBitmap(width, height)

            // Get pixel data
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // ENHANCED SHARPENING KERNEL
            // Increased center weight for stronger sharpening
            // Kernel: [0, -1, 0]
            //         [-1, 6, -1]  (increased from 5 to 6)
            //         [0, -1, 0]
            val sharpenedPixels = IntArray(width * height)

            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    val index = y * width + x

                    val pixel = pixels[index]
                    val top = pixels[(y - 1) * width + x]
                    val bottom = pixels[(y + 1) * width + x]
                    val left = pixels[y * width + (x - 1)]
                    val right = pixels[y * width + (x + 1)]

                    // Apply enhanced kernel to each color channel
                    val alpha = android.graphics.Color.alpha(pixel)

                    // INCREASED CENTER WEIGHT from 5 to 6 for stronger sharpening
                    var red = 6 * android.graphics.Color.red(pixel) -
                            android.graphics.Color.red(top) -
                            android.graphics.Color.red(bottom) -
                            android.graphics.Color.red(left) -
                            android.graphics.Color.red(right)

                    var green = 6 * android.graphics.Color.green(pixel) -
                            android.graphics.Color.green(top) -
                            android.graphics.Color.green(bottom) -
                            android.graphics.Color.green(left) -
                            android.graphics.Color.green(right)

                    var blue = 6 * android.graphics.Color.blue(pixel) -
                            android.graphics.Color.blue(top) -
                            android.graphics.Color.blue(bottom) -
                            android.graphics.Color.blue(left) -
                            android.graphics.Color.blue(right)

                    // Clamp values between 0 and 255
                    red = red.coerceIn(0, 255)
                    green = green.coerceIn(0, 255)
                    blue = blue.coerceIn(0, 255)

                    sharpenedPixels[index] = android.graphics.Color.argb(alpha, red, green, blue)
                }
            }

            // Copy edge pixels as-is
            for (x in 0 until width) {
                sharpenedPixels[x] = pixels[x] // Top row
                sharpenedPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x] // Bottom row
            }
            for (y in 0 until height) {
                sharpenedPixels[y * width] = pixels[y * width] // Left column
                sharpenedPixels[y * width + width - 1] = pixels[y * width + width - 1] // Right column
            }

            sharpenedBitmap.setPixels(sharpenedPixels, 0, width, 0, 0, width, height)

            FinTrackLogger.d("FinTrack_Image", "Enhanced sharpening completed successfully")
            return sharpenedBitmap
        } catch (e: Exception) {
            FinTrackLogger.d("FinTrack_Image", "Sharpening failed, returning original: ${e.message}")
            return bitmap
        }
    }

    /**
     * OPTIONAL: Add adaptive thresholding for even better small text detection
     * This binarizes the image (pure black/white) which helps OCR significantly
     */
     fun applyAdaptiveThreshold(bitmap: Bitmap): Bitmap {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val thresholdedPixels = IntArray(width * height)
            val blockSize = 15 // Size of neighborhood for threshold calculation

            for (y in 0 until height) {
                for (x in 0 until width) {
                    // Calculate local average
                    var sum = 0
                    var count = 0

                    for (dy in -blockSize/2..blockSize/2) {
                        for (dx in -blockSize/2..blockSize/2) {
                            val ny = (y + dy).coerceIn(0, height - 1)
                            val nx = (x + dx).coerceIn(0, width - 1)
                            val pixel = pixels[ny * width + nx]
                            sum += android.graphics.Color.red(pixel) // Grayscale, so R=G=B
                            count++
                        }
                    }

                    val average = sum / count
                    val currentPixel = pixels[y * width + x]
                    val currentValue = android.graphics.Color.red(currentPixel)

                    // If darker than local average, make it black, else white
                    val newValue = if (currentValue < average - 5) 0 else 255
                    thresholdedPixels[y * width + x] = android.graphics.Color.rgb(newValue, newValue, newValue)
                }
            }

            val resultBitmap = createBitmap(width, height)
            resultBitmap.setPixels(thresholdedPixels, 0, width, 0, 0, width, height)

            FinTrackLogger.d("FinTrack_Image", "Adaptive thresholding completed")
            return resultBitmap
        } catch (e: Exception) {
            FinTrackLogger.d("FinTrack_Image", "Adaptive thresholding failed: ${e.message}")
            return bitmap
        }
    }

}