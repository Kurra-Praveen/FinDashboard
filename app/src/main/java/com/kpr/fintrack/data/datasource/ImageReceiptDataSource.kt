package com.kpr.fintrack.data.datasource

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageReceiptDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processReceipt(imageUri: Uri): ImageProcessingResult = withContext(Dispatchers.IO) {
        com.kpr.fintrack.utils.FinTrackLogger.Receipt.logProcessStart(imageUri.toString())
        try {
            // Create a temporary file in our app's cache directory
            val receiptDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val tempFile = File(receiptDir, "receipt_${System.currentTimeMillis()}.png")

            // Copy the shared content to our temporary file
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Could not open shared image")

            // Create our own content URI using FileProvider
            val tempUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            val inputImage = InputImage.fromFilePath(context, tempUri)
            com.kpr.fintrack.utils.FinTrackLogger.d(IMAGE_TAG, "Created InputImage from temp file")

            val visionText = recognizer.process(inputImage).await()
            com.kpr.fintrack.utils.FinTrackLogger.Receipt.logOcrResult(true, visionText.text)

            // Load bitmap from our temporary file
            val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, tempUri)
            com.kpr.fintrack.utils.FinTrackLogger.d(IMAGE_TAG, "Loaded bitmap: ${bitmap.width}x${bitmap.height}")

            val base64Image = convertBitmapToBase64(bitmap)
            com.kpr.fintrack.utils.FinTrackLogger.d(IMAGE_TAG, "Converted image to base64 (length: ${base64Image.length})")

            // Clean up temporary file
            tempFile.delete()

            return@withContext ImageProcessingResult.Success(
                text = visionText.text,
                base64Image = base64Image
            )
        } catch (e: Exception) {
            com.kpr.fintrack.utils.FinTrackLogger.Receipt.logOcrResult(false, error = e.message)
            return@withContext ImageProcessingResult.Error(e)
        }
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    companion object {
        private const val IMAGE_TAG = "FinTrack_ImageDS"
    }
}

sealed class ImageProcessingResult {
    data class Success(
        val text: String,
        val base64Image: String
    ) : ImageProcessingResult()

    data class Error(val exception: Exception) : ImageProcessingResult()
}
