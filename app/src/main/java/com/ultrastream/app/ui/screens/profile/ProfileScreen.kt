package com.ultrastream.app.ui.screens.profile

import kotlinx.coroutines.launch

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrastream.app.ui.components.AnalyticsCard

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedRating by remember { mutableStateOf(false) }
    var expandedLanguage by remember { mutableStateOf(false) }
    var showNewProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf(\"\") }
    var expandedRating by remember { mutableStateOf(false) }
    var expandedLanguage by remember { mutableStateOf(false) }
    var showNewProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf(\"\") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val success = viewModel.importData(context, uri)
                if (success) {
                    Toast.makeText(context, "Data imported successfully", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Import failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        // Analytics Dashboard
        item {
            Text("Analytics", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsCard(label = "Watched", value = uiState.watchedCount.toString())
                AnalyticsCard(label = "In Progress", value = uiState.inProgressCount.toString())
                AnalyticsCard(label = "Library", value = uiState.libraryCount.toString())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsCard(label = "Watchlist", value = uiState.watchlistCount.toString())
                AnalyticsCard(label = "History", value = uiState.historyCount.toString())
                AnalyticsCard(label = "Completion", value = uiState.completionRate.toString() + "%")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        // Analytics Dashboard
        item {
            Text("Analytics", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsCard(label = "Watched", value = uiState.watchedCount.toString())
                AnalyticsCard(label = "In Progress", value = uiState.inProgressCount.toString())
                AnalyticsCard(label = "Library", value = uiState.libraryCount.toString())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsCard(label = "Watchlist", value = uiState.watchlistCount.toString())
                AnalyticsCard(label = "History", value = uiState.historyCount.toString())
                AnalyticsCard(label = "Completion", value = uiState.completionRate.toString() + "%")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Theme")
                Switch(
                    checked = uiState.theme == "light",
                    onCheckedChange = {
                        scope.launch { viewModel.toggleTheme() }
                    }
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hindi Priority")
                Switch(
                    checked = uiState.hindiPriority,
                    onCheckedChange = {
                        scope.launch { viewModel.toggleHindiPriority() }
                    }
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Auto-play Next")
                Switch(
                    checked = uiState.autoPlayNext,
                    onCheckedChange = {
                        scope.launch { viewModel.toggleAutoPlayNext() }
                    }
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Parental Control")
                Switch(
                    checked = uiState.parentalControl,
                    onCheckedChange = {
                        scope.launch { viewModel.toggleParentalControl() }
                    }
                )
            }
        }
        item {
            Text("Profile: ${uiState.currentProfile}", style = MaterialTheme.typography.bodyLarge)
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        val success = viewModel.exportData(context)
                        if (success) {
                            Toast.makeText(context, "Data exported successfully", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Export failed", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Data")
            }
        }
        item {
            Button(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import Data")
            }
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        viewModel.factoryReset(context)
                        Toast.makeText(context, "Factory reset performed", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Factory Reset")
            }
        }
    }
}
