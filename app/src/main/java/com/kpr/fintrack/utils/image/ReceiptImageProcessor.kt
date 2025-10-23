package com.kpr.fintrack.utils.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.kpr.fintrack.utils.FinTrackLogger
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object ReceiptImageProcessor {
    private const val TAG = "FinTrack_Image"
    private const val MAX_WIDTH = 1080
    private const val JPEG_QUALITY = 85

    /**
     * Compress the input image file (png/jpg/etc) and save a compressed JPEG in app files/receipts.
     * Returns the compressed file or throws an exception on failure.
     */
    fun compressAndSaveToAppStorage(context: Context, inputFile: File): File {
        FinTrackLogger.d(TAG, "compressAndSaveToAppStorage: input=${inputFile.absolutePath}")

        // Decode with inJustDecodeBounds first to compute scale
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(inputFile.absolutePath, opts)
        val (origW, origH) = opts.outWidth to opts.outHeight
        FinTrackLogger.d(TAG, "Original size: ${origW}x${origH}")

        // Determine scale factor
        val scale = if (origW > MAX_WIDTH) origW.toFloat() / MAX_WIDTH else 1f
        val targetW = (origW / scale).roundToInt()
        val targetH = (origH / scale).roundToInt()

        // Decode bitmap with sample size to avoid OOM
        val sampleSize = calculateInSampleSize(opts, targetW, targetH)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        var bitmap = BitmapFactory.decodeFile(inputFile.absolutePath, decodeOpts)
            ?: throw IllegalStateException("Failed to decode bitmap from file: ${inputFile.absolutePath}")

        FinTrackLogger.d(TAG, "Decoded bitmap size: ${bitmap.width}x${bitmap.height}, sampleSize=$sampleSize")

        // Handle EXIF rotation
        try {
            val exif = ExifInterface(inputFile.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotated = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
            if (rotated != bitmap) {
                bitmap.recycle()
                bitmap = rotated
            }
        } catch (e: Exception) {
            FinTrackLogger.w(TAG, "Failed to read EXIF orientation: ${e.message}")
        }

        // Resize if needed to targetW x targetH
        if (bitmap.width > MAX_WIDTH) {
            val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            if (scaled != bitmap) {
                bitmap.recycle()
                bitmap = scaled
            }
        }

        // Prepare destination file
        val receiptsDir = File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }
        val destFile = File(receiptsDir, "receipt_${System.currentTimeMillis()}.jpg")

        // Compress to JPEG
        FileOutputStream(destFile).use { out ->
            val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            if (!ok) throw IllegalStateException("Failed to compress bitmap to JPEG")
        }

        FinTrackLogger.d(TAG, "Compressed image saved to: ${destFile.absolutePath}, size=${destFile.length()} bytes")

        // Cleanup
        bitmap.recycle()

        return destFile
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun decodeFileToBitmap(file: File): Bitmap? {
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            FinTrackLogger.e(TAG, "decodeFileToBitmap failed", e)
            null
        }
    }
}

