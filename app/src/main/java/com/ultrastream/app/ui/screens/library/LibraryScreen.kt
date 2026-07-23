package com.ultrastream.app.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrastream.app.ui.components.SectionHeader
import com.ultrastream.app.ui.components.GridSection
import com.ultrastream.app.ui.components.PosterCard

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onItemClick: (id: String, type: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillParentMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item {
                SectionHeader(title = "Library")
                // Grid for library items
                if (uiState.library.isEmpty()) {
                    Text("No saved items", modifier = Modifier.padding(16.dp))
                } else {
                    // We'll implement a grid in GridSection
                    // For now, just list
                    uiState.library.forEach { item ->
                        Text(item.name)
                    }
                }
            }
            item {
                SectionHeader(title = "Watchlist")
                if (uiState.watchlist.isEmpty()) {
                    Text("No watchlist items", modifier = Modifier.padding(16.dp))
                } else {
                    uiState.watchlist.forEach { item ->
                        Text(item.name)
                    }
                }
            }
            item {
                SectionHeader(title = "History")
                if (uiState.history.isEmpty()) {
                    Text("No history", modifier = Modifier.padding(16.dp))
                } else {
                    uiState.history.forEach { item ->
                        Text(item.name)
                    }
                }
            }
        }
    }
}
