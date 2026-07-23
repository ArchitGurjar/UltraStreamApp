package com.ultrastream.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.data.models.Video
import com.ultrastream.app.ui.components.EpisodeCard
import com.ultrastream.app.ui.components.StreamCard
import com.ultrastream.app.ui.components.bottomsheets.SeasonsSheet
import com.ultrastream.app.ui.components.bottomsheets.StreamsSheet
import com.ultrastream.app.ui.theme.AccentBlue
import com.ultrastream.app.ui.theme.BackgroundDark
import com.ultrastream.app.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
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

    val meta = uiState.meta

    LaunchedEffect(id, type) {
        viewModel.loadMeta(id, type)
    }

    LaunchedEffect(meta) {
        if (meta?.type == "series" || meta?.type == "anime") {
            val seasons = meta.videos?.mapNotNull { it.season }?.distinct()?.sorted() ?: emptyList()
            if (seasons.isNotEmpty() && uiState.selectedSeason == null) {
                viewModel.selectSeason(seasons.first())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        if (meta != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Hero Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    ) {
                        AsyncImage(
                            model = meta.background ?: meta.poster,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            BackgroundDark.copy(alpha = 0.8f),
                                            BackgroundDark
                                        ),
                                        startY = 0.3f
                                    )
                                )
                        )

                        // Top Bar Overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    IconButton(onClick = { /* View History */ }) {
                                        Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    IconButton(onClick = { viewModel.toggleLibrary(meta) }) {
                                        Icon(
                                            imageVector = if (uiState.inLibrary) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (uiState.inLibrary) AccentBlue else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Hero Content
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.Black.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = meta.type.uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = meta.name,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(meta.year ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text("•", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                Text(meta.runtime ?: "N/A", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text("•", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                Text("⭐ ${meta.imdbRating ?: "N/A"}", style = MaterialTheme.typography.bodyMedium, color = AccentBlue, fontWeight = FontWeight.Bold)
                                if (!meta.genre.isNullOrEmpty()) {
                                    Text("•", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                    Text(meta.genre!!.take(2).joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = meta.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                maxLines = 4,
                                softWrap = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            // Cast Chips
                            if (!meta.cast.isNullOrEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    meta.cast!!.take(5).forEach { actor ->
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color.White.copy(alpha = 0.1f),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                        ) {
                                            Text(
                                                text = actor,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Action Buttons
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                viewModel.loadStreams(meta.id, meta.type, uiState.selectedSeason, uiState.selectedEpisode)
                                showStreamsSheet = true
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Satellite, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.streamsLoading) "Loading Streams..." else "Find Streams", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { /* Open IMDb */ },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View on IMDb", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // 3. Episodes Section
                if (meta.type == "series" || meta.type == "anime") {
                    val seasons = meta.videos?.mapNotNull { it.season }?.distinct()?.sorted() ?: emptyList()
                    val episodes = meta.videos
                        ?.filter { it.season == uiState.selectedSeason }
                        ?.sortedBy { it.episode } ?: emptyList()

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episodes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (seasons.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Season ${uiState.selectedSeason ?: ""}", fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }.clickable { showSeasonsSheet = true }
                                }
                            }
                        }
                    }

                    // FIX: NO nested LazyVerticalGrid. Using a Standard Column
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            episodes.forEach { video ->
                                val epKey = "${meta.id}_s${video.season}_e${video.episode}"
                                val isWatched = uiState.watchProgress?.let { 
                                    // Logic to check if episode is fully watched
                                    it.percent >= 100 
                                } ?: false
                                
                                EpisodeCard(
                                    video = video,
                                    isWatched = isWatched,
                                    progressPercent = uiState.watchProgress?.percent ?: 0,
                                    onClick = {
                                        viewModel.selectEpisode(video.episode ?: 0)
                                        viewModel.loadStreams(meta.id, meta.type, uiState.selectedSeason, video.episode)
                                        showStreamsSheet = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

        } else if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    // Bottom Sheets
    if (showSeasonsSheet) {
        val seasons = meta?.videos?.mapNotNull { it.season }?.distinct()?.sorted() ?: emptyList()
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
        // Custom Streams Sheet Implementation using StreamCard
        ModalBottomSheet(
            onDismissRequest = { showStreamsSheet = false }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Available Streams", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(uiState.streams) { stream ->
                        StreamCard(
                            stream = stream,
                            onClick = {
                                showStreamsSheet = false
                                viewModel.playStream(stream, meta?.name ?: "Stream") { resolvedStream, title ->
                                    onPlay(resolvedStream, title)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
