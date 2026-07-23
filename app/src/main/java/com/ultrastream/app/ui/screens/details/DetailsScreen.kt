package com.ultrastream.app.ui.screens.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.ui.components.bottomsheets.StreamsSheet

@Composable
fun DetailsScreen(
    id: String,
    type: String,
    viewModel: DetailsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPlay: (url: String, title: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStreamsSheet by remember { mutableStateOf(false) }

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
                        onClick = { viewModel.toggleLibrary(meta) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (uiState.inLibrary) "Remove from Library" else "Add to Library")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.toggleWatchlist(meta) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (uiState.inWatchlist) "Remove from Watchlist" else "Add to Watchlist")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        // Fetch streams and show sheet
                        viewModel.loadStreams(meta.id, meta.type)
                        showStreamsSheet = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.streamsLoading) "Loading..." else "Find Streams")
                }
            }
            // Episodes if series
            if (meta.videos != null && meta.videos.isNotEmpty()) {
                item {
                    Text("Episodes", style = MaterialTheme.typography.titleLarge)
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

    // Streams bottom sheet
    if (showStreamsSheet && uiState.streams.isNotEmpty()) {
        StreamsSheet(
            streams = uiState.streams,
            onDismiss = { showStreamsSheet = false },
            onStreamClick = { stream ->
                val url = stream.url ?: stream.streamUrl ?: stream.externalUrl
                if (!url.isNullOrBlank()) {
                    showStreamsSheet = false
                    onPlay(url, meta?.name ?: "Stream")
                }
            }
        )
    }
}
