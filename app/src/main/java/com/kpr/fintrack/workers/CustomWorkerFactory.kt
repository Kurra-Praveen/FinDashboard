package com.kpr.fintrack.workers

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomWorkerFactory @Inject constructor(
    private val hiltWorkerFactory: HiltWorkerFactory
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        Log.d("CustomWorkerFactory", "Creating worker: $workerClassName")
        
        return try {
            val worker = hiltWorkerFactory.createWorker(appContext, workerClassName, workerParameters)
            Log.d("CustomWorkerFactory", "Hilt worker created successfully: $workerClassName")
            worker
        } catch (e: Exception) {
            Log.e("CustomWorkerFactory", "Failed to create Hilt worker: $workerClassName", e)
            null
        }
    }
}

