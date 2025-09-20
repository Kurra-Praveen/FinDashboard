package com.kpr.fintrack.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(
    permissionsState: MultiplePermissionsState,
    onPermissionResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        Spacer(modifier = Modifier.height(48.dp))

        // App Icon/Logo
        Card(
            modifier = Modifier.size(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Title
        Text(
            text = "Welcome to FinTrack",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Description
        Text(
            text = "Track your financial transactions automatically by reading SMS messages from banks and payment apps.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Permission explanation cards
        PermissionExplanationCard(
            icon = Icons.AutoMirrored.Filled.Message,
            title = "SMS Access Required",
            description = "FinTrack needs to read SMS messages to automatically detect and categorize your financial transactions from banks and payment apps."
        )

        PermissionExplanationCard(
            icon = Icons.Default.Security,
            title = "Your Privacy is Protected",
            description = "All data is stored locally on your device with encryption. No personal information is sent to external servers."
        )

        Spacer(modifier = Modifier.weight(1f))

        // Permission button
        Button(
            onClick = {
                permissionsState.launchMultiplePermissionRequest()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !permissionsState.allPermissionsGranted
        ) {
            Text(
                text = if (permissionsState.allPermissionsGranted) {
                    "Permissions Granted"
                } else {
                    "Grant SMS Permissions"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (!permissionsState.allPermissionsGranted && permissionsState.shouldShowRationale) {
            TextButton(
                onClick = {
                    // Handle manual permission setup
                }
            ) {
                Text("Set up permissions manually")
            }
        }
    }
}

@Composable
private fun PermissionExplanationCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
