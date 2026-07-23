package com.ultrastream.app.ui.screens.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.ui.components.bottomsheets.SeasonsSheet
import com.ultrastream.app.ui.components.bottomsheets.StreamsSheet

@Composable
fun DetailsScreen(
    id: String,
    type: String,
    viewModel: DetailsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPlay: (stream: StreamItem, title: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSeasonsSheet by remember { mutableStateOf(false) }
    var showStreamsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(id, type) {
        viewModel.loadMeta(id, type)
    }

    LaunchedEffect(uiState.meta) {
        val meta = uiState.meta ?: return@LaunchedEffect
        if (meta.type == "series" || meta.type == "anime") {
            val seasons = meta.videos?.mapNotNull { it.season }?.distinct()?.sorted() ?: emptyList()
            if (seasons.isNotEmpty() && uiState.selectedSeason == null) {
                viewModel.selectSeason(seasons.first())
            }
        }
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
                    Text("⭐ ${meta.imdbRating}", style = MaterialTheme.typography.bodyMedium)
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
            }

            if (meta.type == "series" || meta.type == "anime") {
                val seasons = meta.videos?.mapNotNull { it.season }?.distinct()?.sorted() ?: emptyList()
                val episodes = meta.videos
                    ?.filter { it.season == uiState.selectedSeason }
                    ?.sortedBy { it.episode } ?: emptyList()

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Season ${uiState.selectedSeason ?: ""}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (seasons.isNotEmpty()) {
                            Button(onClick = { showSeasonsSheet = true }) {
                                Text("Change Season")
                            }
                        }
                    }
                }
                if (episodes.isNotEmpty()) {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 80.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            items(episodes) { video ->
                                val epNum = video.episode ?: 0
                                val isSelected = epNum == uiState.selectedEpisode
                                Card(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    onClick = {
                                        viewModel.selectEpisode(epNum)
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "E$epNum",
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.loadStreams(meta.id, meta.type, uiState.selectedSeason, uiState.selectedEpisode)
                        showStreamsSheet = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.streamsLoading) "Loading..." else "Find Streams")
                }
            }
        } else if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (uiState.error != null) {
            item {
                Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showSeasonsSheet) {
        val seasons = uiState.meta?.videos?.mapNotNull { it.season }?.distinct()?.sorted() ?: emptyList()
        SeasonsSheet(
            seasons = seasons,
            currentSeason = uiState.selectedSeason ?: 0,
            onDismiss = { showSeasonsSheet = false },
            onSeasonSelected = { season ->
                viewModel.selectSeason(season)
                showSeasonsSheet = false
            }
        )
    }

    if (showStreamsSheet && uiState.streams.isNotEmpty()) {
        StreamsSheet(
            streams = uiState.streams,
            onDismiss = { showStreamsSheet = false },
            onStreamClick = { stream ->
                showStreamsSheet = false
                viewModel.playStream(stream, meta?.name ?: "Stream") { resolvedStream, title ->
                    onPlay(resolvedStream, title)
                }
            }
        )
    }
}
