package com.ultrastream.app.ui.screens.details

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.data.models.Video
import com.ultrastream.app.ui.components.bottomsheets.StreamsSheet
import com.ultrastream.app.ui.theme.*
import com.ultrastream.app.utils.M3UExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(
    id: String,
    type: String,
    viewModel: DetailsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPlay: (stream: StreamItem, title: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var showSeasonsSheet by remember { mutableStateOf(false) }
    var showStreamsSheet by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }
    var selectedStream by remember { mutableStateOf<StreamItem?>(null) }

    val meta = uiState.meta
    val filteredEpisodes by viewModel.filteredEpisodes.collectAsState()
    val availableSeasons by viewModel.availableSeasons.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val isAllSeasons by viewModel.isAllSeasons.collectAsState()

    LaunchedEffect(id, type) {
        viewModel.loadMeta(id, type)
    }

    LaunchedEffect(meta) {
        if (meta?.type == "series" || meta?.type == "anime") {
            val seasons = meta.videos?.mapNotNull { it.season }?.distinct()?.sorted() ?: emptyList()
            if (seasons.isNotEmpty() && uiState.selectedSeason == null) {
                viewModel.selectSeasonAndLoad(seasons.first())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        if (meta != null) {
            // ROOT: LazyVerticalGrid with adaptive columns
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Hero Section (Full width)
                item(span = { GridItemSpan(maxLineSpan) }) {
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
                                    IconButton(onClick = { viewModel.toggleWatchlist(meta) }) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "Watchlist",
                                            tint = if (uiState.inWatchlist) AccentBlue else Color.White
                                        )
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
                                            contentDescription = "Library",
                                            tint = if (uiState.inLibrary) AccentBlue else Color.White
                                        )
                                    }
                                }
                            }
                        }

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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(meta.year ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text("•", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                Text(meta.runtime ?: "N/A", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text("•", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                Text(
                                    "⭐ ${meta.imdbRating ?: "N/A"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!meta.genre.isNullOrEmpty()) {
                                    Text("•", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                    Text(
                                        meta.genre!!.take(2).joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
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

                // 2. Find Streams Button (Full width)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                viewModel.loadStreams(
                                    meta.id,
                                    meta.type,
                                    uiState.selectedSeason,
                                    uiState.selectedEpisode
                                )
                                showStreamsSheet = true
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Satellite, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (uiState.streamsLoading) "Loading Streams..." else "Find Streams",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val imdbId = meta.imdbId ?: meta.imdb_id
                                if (!imdbId.isNullOrBlank()) {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.imdb.com/title/$imdbId"))
                                    )
                                }
                            },
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

                // 3. Season selector (Full width)
                if (meta.type == "series" || meta.type == "anime") {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = isAllSeasons,
                                onClick = { viewModel.toggleAllSeasons() },
                                label = { Text("All Seasons") },
                                modifier = Modifier.padding(vertical = 4.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            availableSeasons.forEach { season ->
                                FilterChip(
                                    selected = season == selectedSeason && !isAllSeasons,
                                    onClick = { viewModel.selectSeasonAndLoad(season) },
                                    label = { Text("S$season") },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }

                    // 4. Episode cards (Grid items, no span – adaptive columns)
                    items(filteredEpisodes) { video ->
                        val epNum = video.episode ?: 0
                        val isSelected = epNum == uiState.selectedEpisode
                        val isWatched = uiState.watchProgress?.percent?.let { it >= 100 } ?: false
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                else if (isWatched) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            onClick = {
                                viewModel.selectEpisode(epNum)
                                viewModel.loadStreams(
                                    meta.id,
                                    meta.type,
                                    video.season,
                                    epNum
                                )
                                showStreamsSheet = true
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "E${epNum.toString().padStart(2, '0')}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    video.name?.let { name ->
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isWatched && !isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Watched",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Footer spacer (Full width)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(32.dp))
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

    // Streams bottom sheet (old)
    if (showStreamsSheet && uiState.streams.isNotEmpty()) {
        StreamsSheet(
            streams = uiState.streams,
            onDismiss = { showStreamsSheet = false },
            onStreamClick = { stream ->
                showStreamsSheet = false
                selectedStream = stream
                showActionSheet = true
            }
        )
    }

    // New Stream Action Sheet
    if (showActionSheet && selectedStream != null) {
        val stream = selectedStream!!
        val title = meta?.name ?: "Stream"
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Stream Options",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Play
                Button(
                    onClick = {
                        showActionSheet = false
                        viewModel.playStream(stream, title) { resolved, t ->
                            onPlay(resolved, t)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play in Default Player", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Other actions in two-column grid
                val actions = mutableListOf<Pair<String, () -> Unit>>()

                // Smart Playlist (only if episode info)
                if (uiState.selectedEpisode != null && uiState.selectedSeason != null && meta != null) {
                    actions.add("Make Smart Playlist" to {
                        scope.launch {
                            val success = viewModel.createSmartPlaylist(meta, uiState.selectedSeason!!)
                            if (success) {
                                Toast.makeText(context, "Smart Playlist created!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to create playlist", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }

                actions.addAll(listOf(
                    "Search Subtitles" to {
                        scope.launch {
                            if (uiState.selectedSeason != null && uiState.selectedEpisode != null && meta != null) {
                                val subs = viewModel.fetchSubtitles(
                                    meta.id,
                                    meta.type,
                                    uiState.selectedSeason!!,
                                    uiState.selectedEpisode!!
                                )
                                if (subs.isNotEmpty()) {
                                    Toast.makeText(context, "✅ ${subs.size} subtitles found!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No subtitles available", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Select an episode first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    "Download / Open Browser" to {
                        val url = stream.url ?: stream.streamUrl ?: stream.externalUrl
                        if (!url.isNullOrBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(Intent.createChooser(intent, "Open with"))
                        } else {
                            Toast.makeText(context, "No URL available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    "Copy Magnet Link" to {
                        val magnet = if (stream.url?.startsWith("magnet:") == true) stream.url
                        else if (stream.infoHash != null) "magnet:?xt=urn:btih:${stream.infoHash}"
                        else null
                        if (magnet != null) {
                            clipboard.setText(AnnotatedString(magnet))
                            Toast.makeText(context, "Magnet copied to clipboard", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No magnet link available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    "Copy Video URL" to {
                        val url = stream.url ?: stream.streamUrl ?: stream.externalUrl
                        if (!url.isNullOrBlank()) {
                            clipboard.setText(AnnotatedString(url))
                            Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No URL available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    "Export .m3u" to {
                        val url = stream.url ?: stream.streamUrl ?: stream.externalUrl
                        if (!url.isNullOrBlank() && !url.startsWith("magnet:")) {
                            val exporter = M3UExporter(context)
                            val file = exporter.exportToM3U(listOf(stream), title)
                            if (file != null) {
                                exporter.shareM3U(file)
                            } else {
                                Toast.makeText(context, "Failed to create M3U", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Cannot export magnet or empty URL", Toast.LENGTH_SHORT).show()
                        }
                    }
                ))

                actions.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (label, action) ->
                            OutlinedButton(
                                onClick = {
                                    action()
                                    showActionSheet = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
