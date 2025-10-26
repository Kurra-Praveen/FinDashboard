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
import com.kpr.fintrack.utils.image.ImageUtils

@Singleton
class ImageReceiptDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processReceipt(imageUri: Uri): ImageProcessingResult = withContext(Dispatchers.IO) {
        //FinTrackLogger.Receipt.logProcessStart(imageUri.toString())
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
            val preprocessedBitmap = ImageUtils.preprocessImageForOcrNative(originalBitmap)
            savePreprocessedToGallery(preprocessedBitmap)
            val inputImage = InputImage.fromBitmap(preprocessedBitmap, 0)

            FinTrackLogger.d("FinTrack_Image", "Starting OCR on original image: ${tempFile.absolutePath}")
            val visionText = recognizer.process(inputImage).await()
            val orderedText = ImageUtils.sortTextBlocksByPosition(visionText)
            FinTrackLogger.Receipt.logOcrResult(true, orderedText)

            // Release the bitmap to free memory
            originalBitmap.recycle()

            // Step 2: Now compress and save the image to app storage
            val compressedFile = ReceiptImageProcessor.compressAndSaveToAppStorage(context, tempFile)
            FinTrackLogger.d("FinTrack_Image", "Image compressed and saved: ${compressedFile.absolutePath}")

            return@withContext ImageProcessingResult.Success(
                text = orderedText,
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
