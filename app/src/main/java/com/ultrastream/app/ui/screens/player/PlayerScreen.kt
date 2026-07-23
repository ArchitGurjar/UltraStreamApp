package com.ultrastream.app.ui.screens.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView

@Composable
fun PlayerScreen(
    url: String,
    title: String = "Now Playing",
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current

    LaunchedEffect(url) {
        viewModel.initializePlayer(url, title)
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            viewModel.releasePlayer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false // we use custom controls
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { playerView ->
            // Link player
            // We'll get the player from viewModel, but we need to expose it.
            // For simplicity, we'll set it directly.
            // In a real app, we'd expose a function in ViewModel.
        }

        // Custom controls overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar with title and back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(uiState.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = { /* close/back */ }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Progress bar (will be updated with player position)
            // For now placeholder
            LinearProgressIndicator(
                progress = 0.5f,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { viewModel.skipBackward() }
                ) {
                    Icon(Icons.Default.Replay, contentDescription = "Back 10s", tint = Color.White)
                }
                IconButton(
                    onClick = { viewModel.playPause() }
                ) {
                    Icon(
                        if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = { viewModel.skipForward() }
                ) {
                    Icon(Icons.Default.Forward, contentDescription = "Forward 10s", tint = Color.White)
                }
                IconButton(
                    onClick = { viewModel.toggleFullscreen() }
                ) {
                    Icon(
                        if (uiState.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }

        // Gesture overlay for volume/brightness (swipe left/right)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaX = dragAmount.x
                        if (change.position.x < size.width / 2) {
                            // Brightness on left side
                            val newBrightness = (uiState.brightness + deltaX / size.width).coerceIn(0f, 1f)
                            viewModel.setBrightness(newBrightness)
                        } else {
                            // Volume on right side
                            val newVolume = (uiState.volume + deltaX / size.width).coerceIn(0f, 1f)
                            viewModel.setVolume(newVolume)
                        }
                    }
                }
        )
    }
}
