package com.kpr.fintrack.presentation.ui.settings.categeorySettings


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryFormScreen(
    uiState: CategoryFormUiState,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onFormStateChanged: (CategoryFormData) -> Unit,
    onKeywordAdded: () -> Unit,
    onKeywordRemoved: (String) -> Unit
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