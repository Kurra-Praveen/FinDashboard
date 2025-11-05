package com.kpr.fintrack.presentation.ui.settings.categeorySettings


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryFormScreen(
    uiState: CategoryFormUiState,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onFormStateChanged: (CategoryFormData) -> Unit,
    onKeywordAdded: () -> Unit,
    onKeywordRemoved: (String) -> Unit,
    onShowColorPicker: () -> Unit, // (NEW) Add these
    onDismissColorPicker: () -> Unit, // (NEW)
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Category" else "Add Category") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Done, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (uiState.isColorPickerVisible) {
                FullColorPickerDialog(
                    initialColorHex = uiState.form.color,
                    onDismiss = onDismissColorPicker,
                    onColorSelected = { newColorHex ->
                        onFormStateChanged(uiState.form.copy(color = newColorHex))
                        onDismissColorPicker()
                    }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- Category Name ---
                OutlinedTextField(
                    value = uiState.form.name,
                    onValueChange = {
                        onFormStateChanged(uiState.form.copy(name = it))
                    },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                // --- Icon Picker ---
                Text("Select Icon", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.defaultIcons.forEach { icon ->
                        FilterChip(
                            selected = (uiState.form.icon == icon),
                            onClick = { onFormStateChanged(uiState.form.copy(icon = icon)) },
                            label = { Text(icon, fontSize = 20.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                CurrentColorDisplay(
                    colorHex = uiState.form.color,
                    onClick = onShowColorPicker // Call the ViewModel to show the dialog
                )
                Spacer(Modifier.height(24.dp))

                // --- Keyword Manager ---
                Text("Matching Keywords", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.form.currentKeywordInput,
                    onValueChange = {
                        onFormStateChanged(uiState.form.copy(currentKeywordInput = it))
                    },
                    label = { Text("Add a keyword (e.g., 'Groceries', 'Zomato')") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = onKeywordAdded) {
                            Icon(Icons.Default.Add, "Add Keyword")
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.form.keywords.forEach { keyword ->
                        InputChip(
                            selected = false,
                            onClick = { onKeywordRemoved(keyword) },
                            label = { Text(keyword) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, "Remove")
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun CurrentColorDisplay(
    colorHex: String,
    onClick: () -> Unit
) {
    // Use a default color if the hex is invalid or empty
    val color = try {
        Color(colorHex.toColorInt())
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // You could add an "edit" icon here if you want
    }
}

// (NEW) The full-screen dialog using the library
@Composable
private fun FullColorPickerDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val controller = rememberColorPickerController()

    // Set the initial color
    val initialColor = try {
        Color(initialColorHex.toColorInt())
    } catch (e: Exception) {
        Color.Red // Default to red if parse fails
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("Select a Color") },
        text = {
            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp),
                controller = controller
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Send the selected color back as a Hex string
                    val hexString = controller.selectedColor.value.toHexCode()
                    onColorSelected(hexString)
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// (NEW) Helper function to convert Compose Color to Hex
private fun Color.toHexCode(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}