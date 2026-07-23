package com.ultrastream.app.ui.screens.addons

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun AddonsScreen(
    viewModel: AddonsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var addonUrl by remember { mutableStateOf("") }
    var debridKey by remember { mutableStateOf(uiState.debridKey) }
    // New Import JSON Text Field State
    var jsonInputText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Addons", style = MaterialTheme.typography.headlineMedium)
        }
        
        // Addon URL Installation
        item {
            OutlinedTextField(
                value = addonUrl,
                onValueChange = { addonUrl = it },
                label = { Text("Manifest URL (https:// or stremio://)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        val success = viewModel.installAddon(addonUrl)
                        if (success) {
                            addonUrl = ""
                            Toast.makeText(context, "✅ Addon Installed!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "❌ Install Failed: Check URL format.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Install Addon")
            }
        }

        // Import & Export exact Stremio JSON Array
        item {
            Text("Sync / Backup", style = MaterialTheme.typography.titleMedium)
            
            // JSON Input Field (Expands dynamically to fit text)
            OutlinedTextField(
                value = jsonInputText,
                onValueChange = { jsonInputText = it },
                label = { Text("Paste Import JSON here") },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                minLines = 3,
                maxLines = 15
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val json = viewModel.exportAddonsJson()
                        if (json.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "✅ JSON Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export JSON")
                }
                Button(
                    onClick = {
                        // STRICT: Only read from the TextField state, NO Clipboard fallback
                        if (jsonInputText.isBlank()) {
                            Toast.makeText(context, "Import JSON field is empty!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            val success = viewModel.importAddonsJson(jsonInputText)
                            if (success) {
                                jsonInputText = ""
                                Toast.makeText(context, "✅ Addons Imported!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "❌ Invalid JSON format.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Import JSON")
                }
            }
        }

        // Debrid Settings
        item {
            Text("Real-Debrid Key", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = debridKey,
                onValueChange = { debridKey = it },
                label = { Text("Debrid API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        viewModel.saveDebridKey(debridKey)
                        Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Debrid Key")
            }
        }

        // Installed Addons List
        item {
            Text("Installed Addons (${uiState.addons.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(uiState.addons.size) { index ->
            val addon = uiState.addons[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(addon.name, style = MaterialTheme.typography.titleSmall)
                        Text(addon.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Switch(
                            checked = addon.enabled,
                            onCheckedChange = {
                                scope.launch {
                                    viewModel.toggleAddon(addon.id, it)
                                }
                            }
                        )
                        if (!addon.required) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.removeAddon(addon.id)
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}
