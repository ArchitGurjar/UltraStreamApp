package com.ultrastream.app.ui.screens.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DetailsScreen(
    id: String,
    type: String,
    viewModel: DetailsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPlay: (url: String, title: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(id, type) {
        viewModel.loadMeta(id, type)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        val meta = uiState.meta
        if (meta != null) {
            item {
                Text(meta.name, style = MaterialTheme.typography.headlineMedium)
                if (meta.year != null) {
                    Text(meta.year, style = MaterialTheme.typography.bodyMedium)
                }
                if (meta.imdbRating != null) {
                    Text("⭐ $meta.imdbRating", style = MaterialTheme.typography.bodyMedium)
                }
                Text(meta.description ?: "", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = {
                            viewModel.toggleLibrary(meta)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (uiState.inLibrary) "Remove from Library" else "Add to Library")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.toggleWatchlist(meta)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (uiState.inWatchlist) "Remove from Watchlist" else "Add to Watchlist")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        // Load streams
                        viewModel.loadStreams(meta.id, meta.type)
                        // For now, we just use a dummy URL
                        onPlay("dummy_url", meta.name)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Find Streams")
                }
            }
            // Episodes if series
            if (meta.videos != null && meta.videos.isNotEmpty()) {
                item {
                    Text("Episodes", style = MaterialTheme.typography.titleLarge)
                    // We'll show a simple list for now
                    meta.videos?.forEach { video ->
                        Text("S${video.season}E${video.episode} - ${video.name ?: "Episode"}")
                    }
                }
            }
        } else if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (uiState.error != null) {
            item {
                Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
