package com.kpr.fintrack.services.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kpr.fintrack.utils.logging.SecureLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val secureLogger: SecureLogger
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            secureLogger.d("TestWorker", "Test worker executed successfully!")
            Result.success()
        } catch (e: Exception) {
            secureLogger.e("TestWorker", "Test worker failed", e)
            Result.failure()
        }
    }
}

