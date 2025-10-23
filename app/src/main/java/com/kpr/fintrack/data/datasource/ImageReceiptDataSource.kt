package com.kpr.fintrack.data.datasource

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import com.kpr.fintrack.utils.FinTrackLogger
import com.kpr.fintrack.utils.image.ReceiptImageProcessor
import androidx.core.graphics.createBitmap

@Singleton
class ImageReceiptDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processReceipt(imageUri: Uri): ImageProcessingResult = withContext(Dispatchers.IO) {
        FinTrackLogger.Receipt.logProcessStart(imageUri.toString())
        var tempFile: File? = null

        try {
            // Create a temporary file in our app's cache directory
            val receiptDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            tempFile = File(receiptDir, "receipt_${System.currentTimeMillis()}.tmp")

            // Copy the shared content to our temporary file
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Could not open shared image")

            FinTrackLogger.d("FinTrack_Image", "Copied shared URI to temp file: ${tempFile.absolutePath}")

            // Step 1: Perform OCR on the ORIGINAL image (before compression)
            val originalBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            val preprocessedBitmap = preprocessImageForOcrNative(originalBitmap)
            savePreprocessedToGallery(preprocessedBitmap)
            val inputImage = InputImage.fromBitmap(preprocessedBitmap, 0)

            FinTrackLogger.d("FinTrack_Image", "Starting OCR on original image: ${tempFile.absolutePath}")
            val visionText = recognizer.process(inputImage).await()
            FinTrackLogger.Receipt.logOcrResult(true, visionText.text)

            // Release the bitmap to free memory
            originalBitmap.recycle()

            // Step 2: Now compress and save the image to app storage
            val compressedFile = ReceiptImageProcessor.compressAndSaveToAppStorage(context, tempFile)
            FinTrackLogger.d("FinTrack_Image", "Image compressed and saved: ${compressedFile.absolutePath}")

            return@withContext ImageProcessingResult.Success(
                text = visionText.text,
                savedFilePath = compressedFile.absolutePath
            )
        } catch (e: Exception) {
            FinTrackLogger.Receipt.logOcrResult(false, error = e.message)
            return@withContext ImageProcessingResult.Error(e)
        } finally {
            // Clean up temporary file in finally block to ensure cleanup
            tempFile?.let {
                try {
                    it.delete()
                    FinTrackLogger.d("FinTrack_Image", "Cleaned up temp file")
                } catch (ignored: Exception) {
                    FinTrackLogger.d("FinTrack_Image", "Failed to delete temp file: ${ignored.message}")
                }
            }
        }
    }

    /**
     * Preprocesses the image for better OCR accuracy using native Android APIs
     * No external dependencies required
     */
    private fun preprocessImageForOcrNative(bitmap: Bitmap): Bitmap {
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

                // Increase contrast
                val contrast = 1.5f
                val scale = contrast + 1f
                val translate = (-.5f * scale + .5f) * 255f

                val contrastMatrix = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))

                grayscaleMatrix.postConcat(contrastMatrix)
                colorFilter = ColorMatrixColorFilter(grayscaleMatrix)
            }

            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            // Apply sharpening
            val sharpenedBitmap = sharpenBitmap(enhancedBitmap)

            // Only recycle if we created a new bitmap
            if (sharpenedBitmap != enhancedBitmap) {
                enhancedBitmap.recycle()
            }

            FinTrackLogger.d("FinTrack_Image", "Native image preprocessing completed successfully")
            return sharpenedBitmap
        } catch (e: Exception) {
            FinTrackLogger.d("FinTrack_Image", "Image preprocessing failed, using original: ${e.message}")
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * Apply sharpening filter to improve text clarity
     */
    private fun sharpenBitmap(bitmap: Bitmap): Bitmap {
        try {
            val width = bitmap.width
            val height = bitmap.height

            // Create output bitmap
            val sharpenedBitmap = createBitmap(width, height)

            // Get pixel data
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // Apply convolution kernel for sharpening
            // Kernel: [0, -1, 0]
            //         [-1, 5, -1]
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

                    // Apply kernel to each color channel
                    val alpha = android.graphics.Color.alpha(pixel)

                    var red = 5 * android.graphics.Color.red(pixel) -
                            android.graphics.Color.red(top) -
                            android.graphics.Color.red(bottom) -
                            android.graphics.Color.red(left) -
                            android.graphics.Color.red(right)

                    var green = 5 * android.graphics.Color.green(pixel) -
                            android.graphics.Color.green(top) -
                            android.graphics.Color.green(bottom) -
                            android.graphics.Color.green(left) -
                            android.graphics.Color.green(right)

                    var blue = 5 * android.graphics.Color.blue(pixel) -
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

            FinTrackLogger.d("FinTrack_Image", "Sharpening completed successfully")
            return sharpenedBitmap
        } catch (e: Exception) {
            FinTrackLogger.d("FinTrack_Image", "Sharpening failed, returning original: ${e.message}")
            return bitmap
        }
    }

    /**
     * Saves the preprocessed bitmap to gallery for debugging
     * Uses PNG format to preserve quality without compression
     */
    fun savePreprocessedToGallery(bitmap: Bitmap, label: String = "preprocessed"): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "${label}_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FinTrack_Debug")
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // PNG with 100% quality
                }
                FinTrackLogger.d("FinTrack_Image", "Saved to gallery: $uri")
            }
            uri
        } catch (e: Exception) {
            FinTrackLogger.d("FinTrack_Image", "Failed to save to gallery: ${e.message}")
            null
        }
    }
}

sealed class ImageProcessingResult {
    data class Success(
        val text: String,
        val savedFilePath: String
    ) : ImageProcessingResult()

    data class Error(val exception: Exception) : ImageProcessingResult()
}
