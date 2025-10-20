package com.kpr.fintrack.utils

import android.util.Log
import com.kpr.fintrack.BuildConfig

object FinTrackLogger {
    private const val DEFAULT_TAG = "FinTrack"
    private const val PARSER_TAG = "FinTrack_Parser"
    private const val IMAGE_TAG = "FinTrack_Image"
    private const val SERVICE_TAG = "FinTrack_Service"

    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, message)
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }

    // Specific logging methods for receipt processing
    object Receipt {
        fun logProcessStart(imageUri: String) {
            d(IMAGE_TAG, "Starting receipt processing for URI: $imageUri")
        }

        fun logOcrResult(success: Boolean, text: String? = null, error: String? = null) {
            if (success) {
                d(IMAGE_TAG, "OCR completed successfully. Extracted text length: ${text?.length}")
                d(IMAGE_TAG, "OCR text preview: ${text?.take(200)}...")
            } else {
                e(IMAGE_TAG, "OCR failed: $error")
            }
        }

        fun logParsingResult(success: Boolean, source: String, details: String) {
            val tag = "$PARSER_TAG:$source"
            if (success) {
                d(tag, "Successfully parsed receipt: $details")
            } else {
                w(tag, "Failed to parse receipt: $details")
            }
        }

        fun logServiceEvent(event: String, details: String? = null) {
            d(SERVICE_TAG, "$event ${details ?: ""}")
        }
    }
}
