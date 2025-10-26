package com.kpr.fintrack.presentation.receivers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.kpr.fintrack.services.ImageImportService
import com.kpr.fintrack.utils.FinTrackLogger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private var pendingImageUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            FinTrackLogger.d(TAG, "All required permissions granted")
            pendingImageUri?.let { uri ->
                handleSharedImage(uri)
            }
        } else {
            FinTrackLogger.w(TAG, "Some permissions were denied")
            showFailureAndFinish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FinTrackLogger.d(TAG, "ShareReceiverActivity onCreate")
        FinTrackLogger.d(TAG, "Intent action: ${intent?.action}, type: ${intent?.type}")

        when {
            intent?.action == Intent.ACTION_SEND &&
            intent?.type?.startsWith("image/") == true -> {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)?.let { uri ->
                    checkPermissionsAndProcess(uri)
                } ?: run {
                    FinTrackLogger.e(TAG, "No image URI in intent extras")
                    showFailureAndFinish()
                }
            }
            else -> {
                FinTrackLogger.w(TAG, "Unsupported intent: action=${intent?.action}, type=${intent?.type}")
                showFailureAndFinish()
            }
        }
    }

    private fun checkPermissionsAndProcess(uri: Uri) {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (hasPermissions(requiredPermissions)) {
            handleSharedImage(uri)
        } else {
            pendingImageUri = uri
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun handleSharedImage(uri: Uri) {
        FinTrackLogger.Receipt.logServiceEvent("Received shared image", "URI: $uri")

        // Try to take persistable URI permission to avoid SecurityException when service accesses the URI
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                FinTrackLogger.d(TAG, "Persistable URI permission taken for $uri")
            }
        } catch (e: Exception) {
            FinTrackLogger.w(TAG, "Could not take persistable URI permission: ${e.message}")
        }

        ImageImportService.startService(this, uri)
        finish()
    }

    private fun showFailureAndFinish() {
        // Could show a toast here if needed
        finish()
    }

    companion object {
        private const val TAG = "FinTrack_ShareReceiver"
    }
}
